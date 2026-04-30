package com.splitmoney.application.domain.patterns.strategy;

import com.splitmoney.application.domain.payment.Payment;
import com.splitmoney.application.domain.payment.PaymentMethodType;
import java.util.Map;

public interface PaymentMethodStrategy {
    PaymentMethodType supportedType();

    Map<String, Object> buildGatewayPayload(Payment payment);
}
