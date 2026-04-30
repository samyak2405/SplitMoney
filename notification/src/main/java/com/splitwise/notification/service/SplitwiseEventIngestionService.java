package com.splitwise.notification.service;

import com.splitwise.notification.domain.NotificationEventType;
import com.splitwise.notification.dto.request.InternalNotificationEventRequest;
import com.splitwise.notification.messaging.SplitwiseNotificationEventMessage;
import com.splitwise.notification.persistence.entity.ProcessedEventEntity;
import com.splitwise.notification.persistence.repository.ProcessedEventRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SplitwiseEventIngestionService {
    private final NotificationCommandService notificationCommandService;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public boolean ingest(SplitwiseNotificationEventMessage eventMessage) {
        UUID eventId = eventMessage.eventId();
        UUID recipientUserId = UUID.fromString(eventMessage.recipientUserId());
        String dedupeKey = eventId + ":" + recipientUserId + ":" + eventMessage.eventType().name();
        if (processedEventRepository.existsByDedupeKey(dedupeKey)) {
            return false;
        }

        String title = resolveTitle(eventMessage);
        String body = resolveBody(eventMessage);
        Map<String, Object> payload = eventMessage.payload() == null ? Map.of() : eventMessage.payload();
        notificationCommandService.createNotification(
                new InternalNotificationEventRequest(
                        recipientUserId,
                        mapEventType(eventMessage.eventType()),
                        title,
                        body,
                        payload
                ),
                dedupeKey
        );

        ProcessedEventEntity processedEvent = new ProcessedEventEntity();
        processedEvent.setId(UUID.randomUUID());
        processedEvent.setEventId(eventId);
        processedEvent.setUserId(recipientUserId);
        processedEvent.setEventType(eventMessage.eventType().name());
        processedEvent.setProcessedAt(Instant.now());
        processedEvent.setDedupeKey(dedupeKey);
        processedEventRepository.save(processedEvent);
        return true;
    }

    private NotificationEventType mapEventType(NotificationEventType source) {
        return source;
    }

    private String resolveTitle(SplitwiseNotificationEventMessage eventMessage) {
        return switch (eventMessage.eventType()) {
            case GROUP_MEMBER_ADDED -> "Added to group";
            case GROUP_MEMBER_REMOVED -> "Removed from group";
            case EXPENSE_ADDED_AGAINST_USER -> "New expense added";
            case PAYMENT_RECEIVED -> "Payment received";
            default -> "Splitwise notification";
        };
    }

    private String resolveBody(SplitwiseNotificationEventMessage eventMessage) {
        String actorEmail = eventMessage.actorEmail() == null ? "A group member" : eventMessage.actorEmail();
        String groupName = String.valueOf(eventMessage.payload() != null
                ? eventMessage.payload().getOrDefault("groupName", "your group")
                : "your group");
        return switch (eventMessage.eventType()) {
            case GROUP_MEMBER_ADDED -> actorEmail + " added you to " + groupName + ".";
            case GROUP_MEMBER_REMOVED -> actorEmail + " removed you from " + groupName + ".";
            case EXPENSE_ADDED_AGAINST_USER -> actorEmail + " added an expense against you in " + groupName + ".";
            case PAYMENT_RECEIVED -> actorEmail + " paid you in " + groupName + ".";
            default -> "You have a new update from Splitwise.";
        };
    }
}
