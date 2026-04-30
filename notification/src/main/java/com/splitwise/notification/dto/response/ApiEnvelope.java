package com.splitwise.notification.dto.response;

import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record ApiEnvelope<T>(
        String requestId,
        boolean success,
        String responseCode,
        String responseMessage,
        OffsetDateTime timestamp,
        T data
) {
}
