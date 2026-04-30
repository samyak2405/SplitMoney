package com.splitmoney.application.domain.patterns;

import com.splitmoney.application.domain.patterns.exception.InvalidStateTransitionException;
import com.splitmoney.application.domain.payment.Payment;
import com.splitmoney.application.domain.payment.PaymentStateHistory;
import com.splitmoney.application.domain.payment.PaymentStatus;
import com.splitmoney.application.infra.persistence.PaymentRepository;
import com.splitmoney.application.infra.persistence.PaymentStateHistoryRepository;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentStateMachine {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentStateMachine.class);
    private final PaymentRepository paymentRepository;
    private final PaymentStateHistoryRepository historyRepository;
    private final Map<PaymentStatus, Set<PaymentStatus>> allowedTransitions;

    public PaymentStateMachine(PaymentRepository paymentRepository, PaymentStateHistoryRepository historyRepository) {
        this.paymentRepository = paymentRepository;
        this.historyRepository = historyRepository;
        this.allowedTransitions = new EnumMap<>(PaymentStatus.class);
        allowedTransitions.put(PaymentStatus.INITIATED, EnumSet.of(PaymentStatus.VALIDATING, PaymentStatus.CANCELLED));
        allowedTransitions.put(PaymentStatus.VALIDATING, EnumSet.of(PaymentStatus.PROCESSING, PaymentStatus.FAILED));
        allowedTransitions.put(PaymentStatus.PROCESSING, EnumSet.of(PaymentStatus.PENDING, PaymentStatus.COMPLETED, PaymentStatus.FAILED));
        allowedTransitions.put(PaymentStatus.PENDING, EnumSet.of(PaymentStatus.COMPLETED, PaymentStatus.FAILED));
        allowedTransitions.put(PaymentStatus.COMPLETED, EnumSet.of(PaymentStatus.REFUND_INITIATED));
        allowedTransitions.put(PaymentStatus.FAILED, EnumSet.noneOf(PaymentStatus.class));
        allowedTransitions.put(PaymentStatus.CANCELLED, EnumSet.noneOf(PaymentStatus.class));
        allowedTransitions.put(PaymentStatus.REFUND_INITIATED, EnumSet.of(PaymentStatus.REFUNDED, PaymentStatus.FAILED));
        allowedTransitions.put(PaymentStatus.REFUNDED, EnumSet.noneOf(PaymentStatus.class));
    }

    @Transactional
    public void transition(Payment payment, PaymentStatus toStatus, String reason) {
        PaymentStatus fromStatus = payment.getStatus();
        if (!isAllowed(fromStatus, toStatus)) {
            LOGGER.warn("invalid state transition paymentId={} from={} to={}", payment.getId(), fromStatus, toStatus);
            throw new InvalidStateTransitionException("Invalid state transition: " + fromStatus + " -> " + toStatus);
        }

        OffsetDateTime now = OffsetDateTime.now();
        int updated = paymentRepository.transitionState(
                payment.getId(),
                fromStatus,
                toStatus,
                payment.getVersion(),
                now
        );
        if (updated == 0) {
            LOGGER.warn("state transition conflict paymentId={} expectedStatus={} expectedVersion={} to={}",
                    payment.getId(), fromStatus, payment.getVersion(), toStatus);
            throw new InvalidStateTransitionException("State transition conflict for payment: " + payment.getId());
        }

        payment.setStatus(toStatus);
        payment.setUpdatedAt(now);

        PaymentStateHistory history = new PaymentStateHistory();
        history.setId(UUID.randomUUID());
        history.setPaymentId(payment.getId());
        history.setFromStatus(fromStatus.name());
        history.setToStatus(toStatus.name());
        history.setReason(reason);
        history.setChangedAt(now);
        historyRepository.save(history);
        LOGGER.info("state transitioned paymentId={} from={} to={} reason={}",
                payment.getId(), fromStatus, toStatus, reason);
    }

    public boolean isAllowed(PaymentStatus from, PaymentStatus to) {
        return allowedTransitions.getOrDefault(from, Set.of()).contains(to);
    }

    public Payment requireById(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new InvalidStateTransitionException("Payment not found: " + id));
    }
}
