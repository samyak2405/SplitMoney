package com.javaproject.splitewise.service.impl;

import com.javaproject.splitewise.config.NotificationQueueProperties;
import com.javaproject.splitewise.config.SplitwiseOutboxProperties;
import com.javaproject.splitewise.model.NotificationOutboxEvent;
import com.javaproject.splitewise.repository.NotificationOutboxRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class NotificationOutboxPublisherService {
    private final NotificationOutboxRepository notificationOutboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final NotificationQueueProperties notificationQueueProperties;
    private final SplitwiseOutboxProperties splitwiseOutboxProperties;
    private final Counter outboxPublishedCounter;
    private final Counter outboxFailedCounter;

    public NotificationOutboxPublisherService(
            NotificationOutboxRepository notificationOutboxRepository,
            RabbitTemplate rabbitTemplate,
            NotificationQueueProperties notificationQueueProperties,
            SplitwiseOutboxProperties splitwiseOutboxProperties,
            MeterRegistry meterRegistry
    ) {
        this.notificationOutboxRepository = notificationOutboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.notificationQueueProperties = notificationQueueProperties;
        this.splitwiseOutboxProperties = splitwiseOutboxProperties;
        this.outboxPublishedCounter = meterRegistry.counter("splitwise.notification.outbox.published");
        this.outboxFailedCounter = meterRegistry.counter("splitwise.notification.outbox.failed");
    }

    @Scheduled(fixedDelayString = "${notification.outbox.fixed-delay-ms:3000}")
    public void publishBatch() {
        List<NotificationOutboxEvent> unpublished = notificationOutboxRepository
                .findUnpublished(PageRequest.of(0, splitwiseOutboxProperties.getBatchSize()));
        if (!unpublished.isEmpty()) {
            log.info("splitwise.notification-outbox.publish-batch size={}", unpublished.size());
        }
        unpublished.forEach(this::publishSingle);
    }

    @Transactional
    public void publishSingle(NotificationOutboxEvent event) {
        try {
            CorrelationData correlationData = new CorrelationData(event.getId().toString());
            rabbitTemplate.convertAndSend(
                    notificationQueueProperties.getExchange(),
                    event.getRoutingKey(),
                    event.getPayload(),
                    message -> {
                        message.getMessageProperties().setHeader("X-Correlation-Id", event.getPayload().getRequestId());
                        message.getMessageProperties().setHeader("eventId", event.getEventId().toString());
                        message.getMessageProperties().setHeader("eventType", event.getEventType());
                        return message;
                    },
                    correlationData
            );
            CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
            if (confirm != null && confirm.isAck()) {
                notificationOutboxRepository.markPublished(event.getId(), OffsetDateTime.now());
                outboxPublishedCounter.increment();
                log.info("splitwise.notification-outbox.published eventId={} routingKey={}",
                        event.getEventId(), event.getRoutingKey());
                return;
            }
            outboxFailedCounter.increment();
            log.error("splitwise.notification-outbox.publish-nack eventId={} routingKey={}",
                    event.getEventId(), event.getRoutingKey());
        } catch (Exception ex) {
            outboxFailedCounter.increment();
            log.error("splitwise.notification-outbox.publish-failed eventId={} routingKey={}",
                    event.getEventId(), event.getRoutingKey(), ex);
        }
    }
}
