package com.splitmoney.application.domain.payment;

import com.splitmoney.application.api.dto.CreatePaymentRequest;
import com.splitmoney.application.api.dto.PaymentListResponse;
import com.splitmoney.application.api.dto.PaymentResponse;
import com.splitmoney.application.api.dto.PaymentReturnResponse;
import com.splitmoney.application.domain.idempotency.IdempotencyService;
import com.splitmoney.application.domain.outbox.OutboxService;
import com.splitmoney.application.domain.patterns.PaymentStateMachine;
import com.splitmoney.application.domain.patterns.port.PaymentGatewayPort;
import com.splitmoney.application.domain.patterns.exception.InvalidStateTransitionException;
import com.splitmoney.application.domain.patterns.exception.ResourceNotFoundException;
import com.splitmoney.application.infra.hyperswitch.HyperswitchPaymentResult;
import com.splitmoney.application.infra.kafka.events.PaymentLifecycleEvent;
import com.splitmoney.application.infra.kafka.events.PaymentNotificationEvent;
import com.splitmoney.application.infra.persistence.PaymentRepository;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentService.class);
    private static final String CREATE_ENDPOINT = "POST:/v1/payments";

    private final PaymentRepository paymentRepository;
    private final PaymentStateMachine paymentStateMachine;
    private final PaymentGatewayPort paymentGatewayPort;
    private final IdempotencyService idempotencyService;
    private final OutboxService outboxService;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentStateMachine paymentStateMachine,
            PaymentGatewayPort paymentGatewayPort,
            IdempotencyService idempotencyService,
            OutboxService outboxService
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentStateMachine = paymentStateMachine;
        this.paymentGatewayPort = paymentGatewayPort;
        this.idempotencyService = idempotencyService;
        this.outboxService = outboxService;
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey) {
        Optional<PaymentResponse> existing = idempotencyService.findExistingResponse(
                CREATE_ENDPOINT,
                idempotencyKey,
                request,
                PaymentResponse.class
        );
        if (existing.isPresent()) {
            LOGGER.info("idempotent replay served endpoint={} idempotencyKey={}", CREATE_ENDPOINT, idempotencyKey);
            return existing.get();
        }

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setPayerUserId(request.payerUserId());
        payment.setPayeeUserId(request.payeeUserId());
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency().toUpperCase());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setClientRequestId(request.clientRequestId());
        payment.setReturnUrl(request.returnUrl());
        payment.setCreatedAt(OffsetDateTime.now());
        payment.setUpdatedAt(OffsetDateTime.now());
        paymentRepository.save(payment);
        LOGGER.info("payment created paymentId={} status={} clientRequestId={}",
                payment.getId(), payment.getStatus(), payment.getClientRequestId());

        paymentStateMachine.transition(payment, PaymentStatus.VALIDATING, "request-validated");
        LOGGER.info("payment transitioned paymentId={} status={}", payment.getId(), payment.getStatus());

        paymentStateMachine.transition(payment, PaymentStatus.PROCESSING, "gateway-initiation-started");
        LOGGER.info("payment transitioned paymentId={} status={}", payment.getId(), payment.getStatus());

        try {
            HyperswitchPaymentResult result = paymentGatewayPort.createPayment(payment);
            payment.setHyperswitchPaymentId(result.hyperswitchPaymentId());
            payment.setCheckoutUrl(result.checkoutUrl());
            paymentRepository.save(payment);
            LOGGER.info("payment gateway initiated paymentId={} hyperswitchPaymentId={} checkoutUrlPresent={} mappedStatus={}",
                    payment.getId(), payment.getHyperswitchPaymentId(), payment.getCheckoutUrl() != null, result.status());
            applyGatewayStatus(payment, result.status());
        } catch (RuntimeException ex) {
            LOGGER.warn("payment gateway initiation failed paymentId={} reason={}", payment.getId(), ex.getMessage());
            paymentStateMachine.transition(payment, PaymentStatus.FAILED, "gateway-initiation-failed");
            outboxService.append("payment", payment.getId(), "payment.failed", toLifecycleEvent(payment));
            outboxService.append(
                    "payment",
                    payment.getId(),
                    "payment.notification.failed",
                    new PaymentNotificationEvent(payment.getId(), payment.getPayerUserId(), "PAYMENT_FAILED", "Payment initiation failed")
            );
        }

        PaymentResponse response = toResponse(payment);
        idempotencyService.saveResponse(CREATE_ENDPOINT, idempotencyKey, request, payment.getId(), 201, response);
        LOGGER.info("payment create response stored paymentId={} idempotencyKey={}", payment.getId(), idempotencyKey);
        return response;
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
        LOGGER.info("payment fetched paymentId={} status={} hyperswitchPaymentId={}",
                payment.getId(), payment.getStatus(), payment.getHyperswitchPaymentId());
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse cancelPayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));

        boolean cancellableWithoutGateway = payment.getStatus() == PaymentStatus.INITIATED;
        boolean cancellableViaGateway = (payment.getStatus() == PaymentStatus.PROCESSING
                || payment.getStatus() == PaymentStatus.PENDING)
                && payment.getHyperswitchPaymentId() != null;

        if (!cancellableWithoutGateway && !cancellableViaGateway) {
            throw new InvalidStateTransitionException(
                    "Payment in status " + payment.getStatus() + " cannot be cancelled");
        }

        if (cancellableViaGateway) {
            try {
                paymentGatewayPort.cancelPayment(payment.getHyperswitchPaymentId());
                LOGGER.info("gateway cancel succeeded paymentId={} hyperswitchPaymentId={}",
                        payment.getId(), payment.getHyperswitchPaymentId());
            } catch (RuntimeException ex) {
                LOGGER.warn("gateway cancel failed paymentId={} reason={} — proceeding with local cancel",
                        payment.getId(), ex.getMessage());
            }
        }

        paymentStateMachine.transition(payment, PaymentStatus.CANCELLED, "cancel-requested");
        LOGGER.info("payment cancelled paymentId={}", payment.getId());
        outboxService.append("payment", payment.getId(), "payment.cancelled", toLifecycleEvent(payment));
        outboxService.append(
                "payment",
                payment.getId(),
                "payment.notification.cancelled",
                new PaymentNotificationEvent(payment.getId(), payment.getPayerUserId(), "PAYMENT_CANCELLED", "Payment cancelled")
        );
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentListResponse listPayments(UUID userId, String statusParam, int limit, String cursor) {
        PaymentStatus status = null;
        if (statusParam != null && !statusParam.isBlank()) {
            try { status = PaymentStatus.valueOf(statusParam.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        OffsetDateTime cursorTime = decodeCursor(cursor);
        int fetch = Math.min(limit + 1, 101);
        List<Payment> rows = paymentRepository.listForUser(userId, status, cursorTime, fetch);
        boolean hasNext = rows.size() > limit;
        List<Payment> page = hasNext ? rows.subList(0, limit) : rows;
        String endCursor = page.isEmpty() ? null : encodeCursor(page.get(page.size() - 1).getCreatedAt());
        List<PaymentResponse> responses = page.stream().map(this::toResponse).toList();
        return new PaymentListResponse(responses, new PaymentListResponse.PageInfo(hasNext, endCursor));
    }

    @Transactional(readOnly = true)
    public PaymentReturnResponse getReturnStatus(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        String message = switch (payment.getStatus()) {
            case COMPLETED -> "Payment successful";
            case FAILED    -> "Payment failed";
            case CANCELLED -> "Payment cancelled";
            case REFUND_INITIATED -> "Refund in progress";
            case REFUNDED  -> "Payment refunded";
            default        -> "Payment is being processed. Please wait…";
        };
        return new PaymentReturnResponse(paymentId, payment.getStatus().name(), message);
    }

    private String encodeCursor(OffsetDateTime createdAt) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(createdAt.toString().getBytes());
    }

    private OffsetDateTime decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor));
            return OffsetDateTime.parse(decoded);
        } catch (Exception ex) {
            return null;
        }
    }

    public PaymentLifecycleEvent toLifecycleEvent(Payment payment) {
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

    private void applyGatewayStatus(Payment payment, PaymentStatus mappedStatus) {
        if (mappedStatus == PaymentStatus.PENDING) {
            paymentStateMachine.transition(payment, PaymentStatus.PENDING, "gateway-pending");
            outboxService.append("payment", payment.getId(), "payment.pending", toLifecycleEvent(payment));
            return;
        }
        if (mappedStatus == PaymentStatus.COMPLETED) {
            paymentStateMachine.transition(payment, PaymentStatus.COMPLETED, "gateway-completed");
            outboxService.append("payment", payment.getId(), "payment.completed", toLifecycleEvent(payment));
            outboxService.append(
                    "payment",
                    payment.getId(),
                    "payment.notification.completed",
                    new PaymentNotificationEvent(payment.getId(), payment.getPayerUserId(), "PAYMENT_COMPLETED", "Payment completed")
            );
            return;
        }
        if (mappedStatus == PaymentStatus.FAILED) {
            paymentStateMachine.transition(payment, PaymentStatus.FAILED, "gateway-failed");
            outboxService.append("payment", payment.getId(), "payment.failed", toLifecycleEvent(payment));
            outboxService.append(
                    "payment",
                    payment.getId(),
                    "payment.notification.failed",
                    new PaymentNotificationEvent(payment.getId(), payment.getPayerUserId(), "PAYMENT_FAILED", "Payment failed")
            );
            return;
        }
        // PROCESSING is already set. Keep it for eventual webhook/reconciliation completion.
        outboxService.append("payment", payment.getId(), "payment.processing", toLifecycleEvent(payment));
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPayerUserId(),
                payment.getPayeeUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getHyperswitchPaymentId(),
                payment.getReturnUrl(),
                payment.getCheckoutUrl(),
                payment.getUpdatedAt()
        );
    }
}
