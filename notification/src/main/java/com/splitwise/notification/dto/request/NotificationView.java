package com.splitwise.notification.dto.request;

import com.splitwise.notification.domain.NotificationEventType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationView(
        UUID id,
        NotificationEventType eventType,
        String title,
        String body,
        Map<String, Object> payload,
        Instant createdAt,
        boolean isRead,
        Instant readAt,
        boolean isClicked,
        Instant clickedAt
) {
}
