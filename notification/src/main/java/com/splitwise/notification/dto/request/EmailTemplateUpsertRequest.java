package com.splitwise.notification.dto.request;

import com.splitwise.notification.domain.NotificationEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmailTemplateUpsertRequest(
        @NotNull NotificationEventType eventType,
        @NotBlank String subjectTemplate,
        @NotBlank String bodyTemplate,
        boolean html,
        boolean active
) {
}
