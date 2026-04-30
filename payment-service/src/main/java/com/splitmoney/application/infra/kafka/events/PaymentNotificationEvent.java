package com.splitmoney.application.infra.kafka.events;

import java.util.UUID;

public record PaymentNotificationEvent(
        UUID paymentId,
        UUID userId,
        String eventType,
        String message
) {
}
