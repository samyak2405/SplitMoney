package com.splitmoney.application.domain.patterns.strategy;

import com.splitmoney.application.domain.payment.PaymentMethodType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PaymentMethodStrategyResolver {
    private final Map<PaymentMethodType, PaymentMethodStrategy> strategyMap;

    public PaymentMethodStrategyResolver(List<PaymentMethodStrategy> strategies) {
        this.strategyMap = new EnumMap<>(PaymentMethodType.class);
        strategies.forEach(strategy -> strategyMap.put(strategy.supportedType(), strategy));
    }

    public PaymentMethodStrategy resolve(PaymentMethodType type) {
        PaymentMethodStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported payment method strategy for type: " + type);
        }
        return strategy;
    }
}
