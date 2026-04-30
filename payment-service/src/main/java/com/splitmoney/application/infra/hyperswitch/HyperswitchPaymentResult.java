package com.splitmoney.application.infra.hyperswitch;

import com.splitmoney.application.domain.payment.PaymentStatus;

public record HyperswitchPaymentResult(
        String hyperswitchPaymentId,
        PaymentStatus status,
        String checkoutUrl
) {
}
