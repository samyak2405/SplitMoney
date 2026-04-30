package com.splitmoney.application.domain.patterns.strategy;

import com.splitmoney.application.domain.payment.Payment;
import com.splitmoney.application.domain.payment.PaymentMethodType;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CardPaymentMethodStrategy implements PaymentMethodStrategy {

    @Override
    public PaymentMethodType supportedType() {
        return PaymentMethodType.CARD;
    }

    @Override
    public Map<String, Object> buildGatewayPayload(Payment payment) {
        Map<String, Object> payload = new HashMap<>();
        // Hyperswitch requires amount in smallest currency unit (paise for INR)
        payload.put("amount", toMinorUnits(payment.getAmount()));
        payload.put("currency", payment.getCurrency());
        payload.put("capture_method", "automatic");
        payload.put("confirm", false);
        payload.put("description", "SplitMoney settlement");
        return payload;
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }
}
