package com.splitwise.notification.dto.request;

import com.splitwise.notification.domain.NotificationEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record InternalNotificationEventRequest(
        @NotNull UUID userId,
        @NotNull NotificationEventType eventType,
        @NotBlank String title,
        @NotBlank String body,
        Map<String, Object> payload
) {
}
