package com.javaproject.splitewise.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserSyncMessage(
        UUID eventId,
        String eventType,
        UUID userId,
        String email,
        String mobile,
        boolean active,
        OffsetDateTime occurredAt
) {}
