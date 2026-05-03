package com.splitmoney.application.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PaymentMetrics {

    private final MeterRegistry registry;

    public PaymentMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordPaymentInitiated(String method, String currency) {
        Counter.builder("payment.initiated")
                .tag("method", method)       // card | upi | net_banking | wallet
                .tag("currency", currency)
                .description("Payment initiations by method and currency")
                .register(registry)
                .increment();
    }

    public void recordPaymentStatusTransition(String fromStatus, String toStatus) {
        Counter.builder("payment.status.transition")
                .tag("from", fromStatus)
                .tag("to", toStatus)         // SUCCEEDED | FAILED | CANCELLED | PENDING | PROCESSING
                .description("Payment status transitions")
                .register(registry)
                .increment();
    }

    public void recordWebhookReceived(String status) {
        Counter.builder("payment.webhook.received")
                .tag("status", status)       // Hyperswitch webhook status string
                .description("Hyperswitch webhook events received")
                .register(registry)
                .increment();
    }

    public void recordGrpcRequest(String method, boolean success) {
        Counter.builder("payment.grpc.requests")
                .tag("method", method)
                .tag("success", String.valueOf(success))
                .description("gRPC requests handled by payment-service")
                .register(registry)
                .increment();
    }

    public void recordOutboxRelayed(String eventType) {
        Counter.builder("payment.outbox.relayed")
                .tag("event_type", eventType)
                .description("Outbox events relayed to Kafka")
                .register(registry)
                .increment();
    }
}
