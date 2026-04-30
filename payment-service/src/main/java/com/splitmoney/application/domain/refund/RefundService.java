package com.splitmoney.application.domain.refund;

import com.splitmoney.application.api.dto.RefundRequest;
import com.splitmoney.application.api.dto.RefundResponse;
import com.splitmoney.application.domain.outbox.OutboxService;
import com.splitmoney.application.domain.patterns.PaymentStateMachine;
import com.splitmoney.application.domain.patterns.exception.InvalidStateTransitionException;
import com.splitmoney.application.domain.patterns.exception.ResourceNotFoundException;
import com.splitmoney.application.domain.patterns.port.PaymentGatewayPort;
import com.splitmoney.application.domain.payment.Payment;
import com.splitmoney.application.domain.payment.PaymentStatus;
import com.splitmoney.application.infra.hyperswitch.HyperswitchRefundResult;
import com.splitmoney.application.infra.persistence.PaymentRepository;
import com.splitmoney.application.infra.persistence.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentGatewayPort paymentGatewayPort;
    private final PaymentStateMachine stateMachine;
    private final OutboxService outboxService;

    @Transactional
    public RefundResponse initiateRefund(UUID paymentId, RefundRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new InvalidStateTransitionException(
                    "Refund is only allowed for COMPLETED payments. Current status: " + payment.getStatus());
        }

        BigDecimal refundAmount = request.amount() != null ? request.amount() : payment.getAmount();
        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new InvalidStateTransitionException(
                    "Refund amount " + refundAmount + " exceeds original payment amount " + payment.getAmount());
        }

        String reason = request.reason() != null ? request.reason() : "REQUESTED_BY_CUSTOMER";

        stateMachine.transition(payment, PaymentStatus.REFUND_INITIATED, "refund-requested");

        Refund refund = new Refund();
        refund.setId(UUID.randomUUID());
        refund.setPaymentId(paymentId);
        refund.setAmount(refundAmount);
        refund.setCurrency(payment.getCurrency());
        refund.setStatus("REFUND_INITIATED");
        refund.setReason(reason);
        refund.setCreatedAt(OffsetDateTime.now());
        refund.setUpdatedAt(OffsetDateTime.now());
        refundRepository.save(refund);

        try {
            HyperswitchRefundResult result = paymentGatewayPort.refundPayment(
                    payment.getHyperswitchPaymentId(), refundAmount, reason);
            refund.setHyperswitchRefundId(result.hyperswitchRefundId());

            if ("succeeded".equalsIgnoreCase(result.status())) {
                refund.setStatus("REFUNDED");
                stateMachine.transition(payment, PaymentStatus.REFUNDED, "gateway-refund-succeeded");
                outboxService.append("payment", payment.getId(), "payment.refunded", refund.getId().toString());
            } else {
                refund.setStatus("REFUND_PENDING");
            }
            refund.setUpdatedAt(OffsetDateTime.now());
            refundRepository.save(refund);
            log.info("refund initiated paymentId={} refundId={} gatewayStatus={}", paymentId, refund.getId(), result.status());
        } catch (RuntimeException ex) {
            log.error("gateway refund call failed paymentId={} refundId={} reason={}", paymentId, refund.getId(), ex.getMessage());
            stateMachine.transition(payment, PaymentStatus.FAILED, "refund-gateway-error");
            refund.setStatus("REFUND_FAILED");
            refund.setUpdatedAt(OffsetDateTime.now());
            refundRepository.save(refund);
        }

        return toResponse(refund);
    }

    private RefundResponse toResponse(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getPaymentId(),
                refund.getAmount(),
                refund.getCurrency(),
                refund.getStatus(),
                refund.getReason(),
                refund.getHyperswitchRefundId(),
                refund.getCreatedAt()
        );
    }
}
