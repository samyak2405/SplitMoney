package com.splitwise.notification.messaging;

import com.splitwise.notification.domain.NotificationEventType;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record SplitwiseNotificationEventMessage(
        UUID eventId,
        String schemaVersion,
        NotificationEventType eventType,
        OffsetDateTime occurredAt,
        String requestId,
        String aggregateType,
        String aggregateId,
        String actorUserId,
        String actorEmail,
        String recipientUserId,
        String recipientEmail,
        Map<String, Object> payload
) {
}
