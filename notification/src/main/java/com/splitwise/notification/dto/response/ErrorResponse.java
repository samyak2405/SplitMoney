package com.splitwise.notification.dto.response;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String error,
        String message,
        int status,
        Instant timestamp,
        Map<String, String> validationErrors
) {
}
