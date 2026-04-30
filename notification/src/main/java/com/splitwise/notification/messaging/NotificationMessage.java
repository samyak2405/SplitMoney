package com.splitwise.notification.messaging;

import com.splitwise.notification.domain.NotificationChannel;
import com.splitwise.notification.domain.NotificationEventType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationMessage(
        UUID notificationId,
        UUID userId,
        NotificationChannel channel,
        NotificationEventType eventType,
        String idempotencyKey,
        int attempt,
        Instant createdAt,
        Map<String, Object> payload
) {
}
