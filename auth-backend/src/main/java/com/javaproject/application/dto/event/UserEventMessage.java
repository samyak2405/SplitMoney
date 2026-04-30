package com.javaproject.application.dto.event;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserEventMessage {
    private UUID eventId;
    private String eventType;
    private UUID userId;
    private String email;
    private String mobile;
    private boolean active;
    private OffsetDateTime occurredAt;
}
