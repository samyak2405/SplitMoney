package com.javaproject.splitewise.service.impl;

import com.javaproject.splitewise.messaging.NotificationEventType;
import com.javaproject.splitewise.messaging.SplitwiseNotificationEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SplitwiseNotificationEventFactoryTest {

    private final SplitwiseNotificationEventFactory factory = new SplitwiseNotificationEventFactory();

    @Test
    void shouldBuildVersionedEventEnvelope() {
        UUID actorUserId = UUID.randomUUID();
        UUID recipientUserId = UUID.randomUUID();

        SplitwiseNotificationEvent event = factory.build(
                NotificationEventType.GROUP_MEMBER_ADDED,
                "req-1",
                "group",
                "42",
                actorUserId,
                "actor@example.com",
                recipientUserId,
                "recipient@example.com",
                Map.of("groupName", "Weekend Trip")
        );

        assertNotNull(event.getEventId());
        assertEquals("v1", event.getSchemaVersion());
        assertEquals(NotificationEventType.GROUP_MEMBER_ADDED, event.getEventType());
        assertEquals("group", event.getAggregateType());
        assertEquals("42", event.getAggregateId());
        assertEquals(actorUserId.toString(), event.getActorUserId());
        assertEquals(recipientUserId.toString(), event.getRecipientUserId());
    }
}
