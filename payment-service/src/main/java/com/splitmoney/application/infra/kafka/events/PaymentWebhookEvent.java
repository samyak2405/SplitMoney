package com.splitmoney.application.infra.kafka.events;

public record PaymentWebhookEvent(
        String webhookId,
        String hyperswitchPaymentId,
        String status
) {
}
