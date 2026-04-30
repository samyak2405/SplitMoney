package com.splitwise.notification.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record MarkReadRequest(
        @NotEmpty List<UUID> ids
) {
}
