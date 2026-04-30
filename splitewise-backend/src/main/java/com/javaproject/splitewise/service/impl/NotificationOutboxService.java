package com.javaproject.splitewise.service.impl;

import com.javaproject.splitewise.config.NotificationQueueProperties;
import com.javaproject.splitewise.messaging.SplitwiseNotificationEvent;
import com.javaproject.splitewise.model.NotificationOutboxEvent;
import com.javaproject.splitewise.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxService {
    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationQueueProperties notificationQueueProperties;

    @Transactional
    public void enqueue(SplitwiseNotificationEvent event, String routingKey) {
        String effectiveRoutingKey = routingKey == null || routingKey.isBlank()
                ? notificationQueueProperties.getRoutingKey()
                : routingKey;
        notificationOutboxRepository.save(NotificationOutboxEvent.builder()
                .id(UUID.randomUUID())
                .eventId(event.getEventId())
                .eventType(event.getEventType().name())
                .routingKey(effectiveRoutingKey)
                .payload(event)
                .createdAt(OffsetDateTime.now())
                .build());
        log.info("splitwise.notification-outbox.enqueued eventId={} eventType={} routingKey={}",
                event.getEventId(), event.getEventType(), effectiveRoutingKey);
    }
}
