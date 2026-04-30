package com.javaproject.splitewise.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SplitwiseNotificationEvent {
    private UUID eventId;
    private String schemaVersion;
    private NotificationEventType eventType;
    private OffsetDateTime occurredAt;
    private String requestId;
    private String aggregateType;
    private String aggregateId;
    private String actorUserId;
    private String actorEmail;
    private String recipientUserId;
    private String recipientEmail;
    private Map<String, Object> payload;
}
