package com.splitwise.notification.service.command;

import com.splitwise.notification.domain.NotificationChannel;
import com.splitwise.notification.domain.NotificationEventType;
import com.splitwise.notification.messaging.NotificationMessage;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageMapper {

    public NotificationMessage message(
            UUID notificationId,
            UUID userId,
            NotificationChannel channel,
            NotificationEventType eventType,
            String idempotencyKey,
            Instant createdAt,
            Map<String, Object> payload
    ) {
        return new NotificationMessage(
                notificationId,
                userId,
                channel,
                eventType,
                idempotencyKey + ":" + notificationId + ":" + channel,
                1,
                createdAt,
                payload
        );
    }

    public Map<String, Object> outboxPayload(NotificationMessage message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("notificationId", message.notificationId().toString());
        payload.put("userId", message.userId().toString());
        payload.put("channel", message.channel().name());
        payload.put("eventType", message.eventType().name());
        payload.put("idempotencyKey", message.idempotencyKey());
        payload.put("attempt", message.attempt());
        payload.put("createdAt", message.createdAt().toString());
        payload.put("payload", message.payload());
        return payload;
    }
}
