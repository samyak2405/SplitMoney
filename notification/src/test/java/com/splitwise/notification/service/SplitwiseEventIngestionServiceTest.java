package com.splitwise.notification.service;

import com.splitwise.notification.domain.NotificationEventType;
import com.splitwise.notification.messaging.SplitwiseNotificationEventMessage;
import com.splitwise.notification.persistence.entity.ProcessedEventEntity;
import com.splitwise.notification.persistence.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SplitwiseEventIngestionServiceTest {

    @Mock
    private NotificationCommandService notificationCommandService;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private SplitwiseEventIngestionService splitwiseEventIngestionService;

    @Test
    void shouldSkipDuplicateEvent() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SplitwiseNotificationEventMessage message = new SplitwiseNotificationEventMessage(
                eventId,
                "v1",
                NotificationEventType.GROUP_MEMBER_ADDED,
                OffsetDateTime.now(),
                "req-1",
                "group",
                "12",
                UUID.randomUUID().toString(),
                "actor@example.com",
                userId.toString(),
                "recipient@example.com",
                Map.of("groupName", "Trip")
        );
        when(processedEventRepository.existsByDedupeKey(ArgumentMatchers.anyString())).thenReturn(true);

        boolean processed = splitwiseEventIngestionService.ingest(message);

        assertFalse(processed);
        verify(notificationCommandService, never()).createNotification(any(), any());
        verify(processedEventRepository, never()).save(any(ProcessedEventEntity.class));
    }

    @Test
    void shouldPersistDedupeRecordWhenEventProcessed() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SplitwiseNotificationEventMessage message = new SplitwiseNotificationEventMessage(
                eventId,
                "v1",
                NotificationEventType.GROUP_MEMBER_ADDED,
                OffsetDateTime.now(),
                "req-1",
                "group",
                "12",
                UUID.randomUUID().toString(),
                "actor@example.com",
                userId.toString(),
                "recipient@example.com",
                Map.of("groupName", "Trip")
        );
        when(processedEventRepository.existsByDedupeKey(ArgumentMatchers.anyString())).thenReturn(false);

        boolean processed = splitwiseEventIngestionService.ingest(message);

        assertTrue(processed);
        verify(notificationCommandService).createNotification(any(), any());
        verify(processedEventRepository).save(any(ProcessedEventEntity.class));
    }
}
