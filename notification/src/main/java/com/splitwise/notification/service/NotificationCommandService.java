package com.splitwise.notification.service;

import com.splitwise.notification.config.NotificationDomainProperties;
import com.splitwise.notification.domain.NotificationChannel;
import com.splitwise.notification.messaging.NotificationMessage;
import com.splitwise.notification.persistence.repository.NotificationChannelStateRepository;
import com.splitwise.notification.persistence.repository.NotificationRepository;
import com.splitwise.notification.persistence.repository.OutboxRepository;
import com.splitwise.notification.persistence.repository.UserNotificationStatusRepository;
import com.splitwise.notification.service.command.ChannelSelectionService;
import com.splitwise.notification.service.command.NotificationFactory;
import com.splitwise.notification.service.command.NotificationMessageMapper;
import com.splitwise.notification.service.command.OutboxEventFactory;
import com.splitwise.notification.dto.request.InternalNotificationEventRequest;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationCommandService {
    private final NotificationRepository notificationRepository;
    private final NotificationChannelStateRepository channelStateRepository;
    private final UserNotificationStatusRepository userStatusRepository;
    private final OutboxRepository outboxRepository;
    private final NotificationFactory notificationFactory;
    private final ChannelSelectionService channelSelectionService;
    private final NotificationMessageMapper messageMapper;
    private final OutboxEventFactory outboxEventFactory;
    private final NotificationDomainProperties domainProperties;

    public NotificationCommandService(
            NotificationRepository notificationRepository,
            NotificationChannelStateRepository channelStateRepository,
            UserNotificationStatusRepository userStatusRepository,
            OutboxRepository outboxRepository,
            NotificationFactory notificationFactory,
            ChannelSelectionService channelSelectionService,
            NotificationMessageMapper messageMapper,
            OutboxEventFactory outboxEventFactory,
            NotificationDomainProperties domainProperties
    ) {
        this.notificationRepository = notificationRepository;
        this.channelStateRepository = channelStateRepository;
        this.userStatusRepository = userStatusRepository;
        this.outboxRepository = outboxRepository;
        this.notificationFactory = notificationFactory;
        this.channelSelectionService = channelSelectionService;
        this.messageMapper = messageMapper;
        this.outboxEventFactory = outboxEventFactory;
        this.domainProperties = domainProperties;
    }

    @Transactional
    public UUID createNotification(InternalNotificationEventRequest request, String idempotencyKey) {
        UUID notificationId = UUID.randomUUID();
        Instant now = Instant.now();
        Map<String, Object> payload = request.payload() == null ? Map.of() : request.payload();

        notificationRepository.save(Objects.requireNonNull(notificationFactory.notification(
                request,
                notificationId,
                now,
                domainProperties.getDefaultTenant(),
                payload
        )));
        userStatusRepository.save(Objects.requireNonNull(notificationFactory.userStatus(request.userId(), notificationId, now)));

        Set<NotificationChannel> channels = channelSelectionService.select(request.userId());
        for (NotificationChannel channel : channels) {
            channelStateRepository.save(Objects.requireNonNull(notificationFactory.channelState(notificationId, channel, now)));

            NotificationMessage message = messageMapper.message(
                    notificationId,
                    request.userId(),
                    channel,
                    request.eventType(),
                    idempotencyKey,
                    now,
                    payload
            );
            outboxRepository.save(Objects.requireNonNull(outboxEventFactory.create(notificationId, message, now)));
        }
        return notificationId;
    }
}
