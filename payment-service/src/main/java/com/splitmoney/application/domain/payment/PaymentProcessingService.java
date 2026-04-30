package com.splitmoney.application.domain.payment;

import com.splitmoney.application.domain.outbox.OutboxService;
import com.splitmoney.application.domain.patterns.PaymentStateMachine;
import com.splitmoney.application.domain.patterns.exception.ResourceNotFoundException;
import com.splitmoney.application.domain.webhook.ProcessedWebhook;
import com.splitmoney.application.infra.hyperswitch.HyperswitchPaymentResult;
import com.splitmoney.application.infra.kafka.events.PaymentLifecycleEvent;
import com.splitmoney.application.infra.kafka.events.PaymentNotificationEvent;
import com.splitmoney.application.infra.kafka.events.PaymentWebhookEvent;
import com.splitmoney.application.infra.persistence.PaymentRepository;
import com.splitmoney.application.infra.persistence.ProcessedWebhookRepository;
import com.splitmoney.application.infra.settlement.SettlementNotifier;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentProcessingService.class);
    private final PaymentRepository paymentRepository;
    private final PaymentStateMachine stateMachine;
    private final PaymentLockService paymentLockService;
    private final com.splitmoney.application.domain.patterns.port.PaymentGatewayPort paymentGatewayPort;
    private final ProcessedWebhookRepository processedWebhookRepository;
    private final OutboxService outboxService;
    private final SettlementNotifier settlementNotifier;

    public PaymentProcessingService(
            PaymentRepository paymentRepository,
            PaymentStateMachine stateMachine,
            PaymentLockService paymentLockService,
            com.splitmoney.application.domain.patterns.port.PaymentGatewayPort paymentGatewayPort,
            ProcessedWebhookRepository processedWebhookRepository,
            OutboxService outboxService,
            SettlementNotifier settlementNotifier
    ) {
        this.paymentRepository = paymentRepository;
        this.stateMachine = stateMachine;
        this.paymentLockService = paymentLockService;
        this.paymentGatewayPort = paymentGatewayPort;
        this.processedWebhookRepository = processedWebhookRepository;
        this.outboxService = outboxService;
        this.settlementNotifier = settlementNotifier;
    }

    @Transactional
    public void processInitiatedPayment(PaymentLifecycleEvent event) {
        LOGGER.info("processing lifecycle event paymentId={} status={}", event.paymentId(), event.status());
        Payment payment = paymentRepository.findById(event.paymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + event.paymentId()));
        if (!paymentLockService.tryLock(payment.getId(), Duration.ofSeconds(30))) {
            LOGGER.warn("payment lock not acquired paymentId={}", payment.getId());
            return;
        }
        try {
            if (payment.getStatus() == PaymentStatus.VALIDATING) {
                stateMachine.transition(payment, PaymentStatus.PROCESSING, "processor-started");
            }
            if (payment.getStatus() == PaymentStatus.PROCESSING) {
                HyperswitchPaymentResult result = paymentGatewayPort.createPayment(payment);
                LOGGER.info("hyperswitch result paymentId={} hyperswitchPaymentId={} mappedStatus={} checkoutUrlPresent={}",
                        payment.getId(), result.hyperswitchPaymentId(), result.status(), result.checkoutUrl() != null);
                payment.setHyperswitchPaymentId(result.hyperswitchPaymentId());
                payment.setCheckoutUrl(result.checkoutUrl());
                paymentRepository.save(payment);
                if (result.status() == PaymentStatus.PENDING) {
                    stateMachine.transition(payment, PaymentStatus.PENDING, "gateway-pending");
                    outboxService.append("payment", payment.getId(), "payment.pending", toLifecycleEvent(payment));
                } else if (result.status() == PaymentStatus.COMPLETED) {
                    stateMachine.transition(payment, PaymentStatus.COMPLETED, "gateway-completed");
                    outboxService.append("payment", payment.getId(), "payment.completed", toLifecycleEvent(payment));
                    enqueueNotification(payment, "payment.notification.completed", "PAYMENT_COMPLETED", "Payment completed");
                } else if (result.status() == PaymentStatus.FAILED) {
                    stateMachine.transition(payment, PaymentStatus.FAILED, "gateway-failed");
                    outboxService.append("payment", payment.getId(), "payment.failed", toLifecycleEvent(payment));
                    enqueueNotification(payment, "payment.notification.failed", "PAYMENT_FAILED", "Payment failed");
                }
            }
        } finally {
            paymentLockService.unlock(payment.getId());
        }
    }

    @Transactional
    public void handleWebhook(PaymentWebhookEvent event) {
        LOGGER.info("processing webhook event webhookId={} hyperswitchPaymentId={} status={}",
                event.webhookId(), event.hyperswitchPaymentId(), event.status());
        if (processedWebhookRepository.existsByWebhookId(event.webhookId())) {
            LOGGER.info("duplicate webhook ignored webhookId={}", event.webhookId());
            return;
        }

        ProcessedWebhook processedWebhook = new ProcessedWebhook();
        processedWebhook.setId(UUID.randomUUID());
        processedWebhook.setWebhookId(event.webhookId());
        processedWebhook.setCreatedAt(OffsetDateTime.now());
        processedWebhookRepository.save(processedWebhook);

        Payment payment = paymentRepository.findByHyperswitchPaymentId(event.hyperswitchPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for hyperswitch id: " + event.hyperswitchPaymentId()));

        String incomingStatus = event.status() == null ? "" : event.status().toUpperCase();
        if ("COMPLETED".equals(incomingStatus) || "SUCCEEDED".equals(incomingStatus)) {
            if (payment.getStatus() == PaymentStatus.PENDING || payment.getStatus() == PaymentStatus.PROCESSING) {
                stateMachine.transition(payment, PaymentStatus.COMPLETED, "webhook-completed");
                outboxService.append("payment", payment.getId(), "payment.completed", toLifecycleEvent(payment));
                enqueueNotification(payment, "payment.notification.completed", "PAYMENT_COMPLETED", "Payment completed");
                settlementNotifier.notifySettlementComplete(payment);
            }
        } else if ("FAILED".equals(incomingStatus)) {
            if (payment.getStatus() == PaymentStatus.PENDING || payment.getStatus() == PaymentStatus.PROCESSING) {
                stateMachine.transition(payment, PaymentStatus.FAILED, "webhook-failed");
                outboxService.append("payment", payment.getId(), "payment.failed", toLifecycleEvent(payment));
                enqueueNotification(payment, "payment.notification.failed", "PAYMENT_FAILED", "Payment failed");
                settlementNotifier.notifySettlementFailed(payment, "webhook-reported-failure");
            }
        }
    }

    @Transactional
    public void reconcileStuckPayments(int staleSeconds) {
        LOGGER.info("starting stale payment reconciliation staleSeconds={}", staleSeconds);
        OffsetDateTime threshold = OffsetDateTime.now().minusSeconds(staleSeconds);
        List<Payment> stuck = paymentRepository.findTop100ByStatusInAndUpdatedAtBefore(
                List.of(PaymentStatus.PROCESSING, PaymentStatus.PENDING),
                threshold
        );
        for (Payment payment : stuck) {
            if (payment.getHyperswitchPaymentId() == null || payment.getHyperswitchPaymentId().isBlank()) {
                continue;
            }
            HyperswitchPaymentResult result = paymentGatewayPort.fetchPayment(payment.getHyperswitchPaymentId());
            LOGGER.info("reconciled payment status paymentId={} hyperswitchPaymentId={} mappedStatus={}",
                    payment.getId(), payment.getHyperswitchPaymentId(), result.status());
            if (result.status() == PaymentStatus.COMPLETED && payment.getStatus() != PaymentStatus.COMPLETED) {
                stateMachine.transition(payment, PaymentStatus.COMPLETED, "reconciled-completed");
                outboxService.append("payment", payment.getId(), "payment.completed", toLifecycleEvent(payment));
                enqueueNotification(payment, "payment.notification.completed", "PAYMENT_COMPLETED", "Payment completed");
                settlementNotifier.notifySettlementComplete(payment);
            } else if (result.status() == PaymentStatus.FAILED && payment.getStatus() != PaymentStatus.FAILED) {
                stateMachine.transition(payment, PaymentStatus.FAILED, "reconciled-failed");
                outboxService.append("payment", payment.getId(), "payment.failed", toLifecycleEvent(payment));
                enqueueNotification(payment, "payment.notification.failed", "PAYMENT_FAILED", "Payment failed");
            }
        }
    }

    private PaymentLifecycleEvent toLifecycleEvent(Payment payment) {
        return new PaymentLifecycleEvent(
                payment.getId(),
                payment.getPayerUserId(),
                payment.getPayeeUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getHyperswitchPaymentId()
        );
    }

    private void enqueueNotification(Payment payment, String eventType, String notificationType, String message) {
        outboxService.append(
                "payment",
                payment.getId(),
                eventType,
                new PaymentNotificationEvent(payment.getId(), payment.getPayerUserId(), notificationType, message)
        );
    }
}
