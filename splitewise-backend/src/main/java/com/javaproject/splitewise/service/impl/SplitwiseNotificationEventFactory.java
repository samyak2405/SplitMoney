package com.javaproject.splitewise.service.impl;

import com.javaproject.splitewise.messaging.NotificationEventType;
import com.javaproject.splitewise.messaging.SplitwiseNotificationEvent;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class SplitwiseNotificationEventFactory {
    public SplitwiseNotificationEvent build(
            NotificationEventType eventType,
            String requestId,
            String aggregateType,
            String aggregateId,
            UUID actorUserId,
            String actorEmail,
            UUID recipientUserId,
            String recipientEmail,
            Map<String, Object> payload
    ) {
        return SplitwiseNotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .schemaVersion("v1")
                .eventType(eventType)
                .occurredAt(OffsetDateTime.now())
                .requestId(requestId)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .actorUserId(actorUserId != null ? actorUserId.toString() : null)
                .actorEmail(actorEmail)
                .recipientUserId(recipientUserId != null ? recipientUserId.toString() : null)
                .recipientEmail(recipientEmail)
                .payload(payload)
                .build();
    }
}
