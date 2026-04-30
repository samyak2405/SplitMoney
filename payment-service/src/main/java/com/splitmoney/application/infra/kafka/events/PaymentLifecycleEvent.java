package com.splitmoney.application.infra.kafka.events;

import com.splitmoney.application.domain.payment.PaymentMethodType;
import com.splitmoney.application.domain.payment.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentLifecycleEvent(
        UUID paymentId,
        UUID payerUserId,
        UUID payeeUserId,
        BigDecimal amount,
        String currency,
        PaymentMethodType paymentMethod,
        PaymentStatus status,
        String hyperswitchPaymentId
) {
}
