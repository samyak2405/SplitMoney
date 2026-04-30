package com.splitmoney.application.api.dto;

import com.splitmoney.application.domain.payment.PaymentMethodType;
import com.splitmoney.application.domain.payment.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID payerUserId,
        UUID payeeUserId,
        BigDecimal amount,
        String currency,
        PaymentMethodType paymentMethod,
        PaymentStatus status,
        String hyperswitchPaymentId,
        String returnUrl,
        String checkoutUrl,
        OffsetDateTime updatedAt
) {
}
