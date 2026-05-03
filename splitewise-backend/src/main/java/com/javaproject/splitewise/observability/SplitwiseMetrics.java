package com.javaproject.splitewise.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class SplitwiseMetrics {

    private final MeterRegistry registry;

    public SplitwiseMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordExpenseCreated(String splitType) {
        Counter.builder("splitwise.expense.created")
                .tag("split_type", splitType)  // EQUAL | EXACT | PERCENTAGE
                .description("Expenses created by split type")
                .register(registry)
                .increment();
    }

    public void recordGroupCreated() {
        Counter.builder("splitwise.group.created")
                .description("Groups created")
                .register(registry)
                .increment();
    }

    public void recordSettlementCalculated(String outcome) {
        Counter.builder("splitwise.settlement.calculated")
                .tag("outcome", outcome)       // initiated | completed | failed
                .description("Settlement calculation events")
                .register(registry)
                .increment();
    }

    public void recordGrpcCallback(String type, boolean success) {
        Counter.builder("splitwise.grpc.callbacks")
                .tag("type", type)             // settlement_complete | settlement_failed
                .tag("success", String.valueOf(success))
                .description("gRPC callback events received from payment-service")
                .register(registry)
                .increment();
    }

    public void recordEventPublished(String eventType) {
        Counter.builder("splitwise.events.published")
                .tag("event_type", eventType)
                .description("RabbitMQ events published by splitewise-backend")
                .register(registry)
                .increment();
    }
}
