package com.splitwise.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitwise.notification.config.NotificationOutboxProperties;
import com.splitwise.notification.config.NotificationQueueProperties;
import com.splitwise.notification.logging.LogContextKeys;
import com.splitwise.notification.logging.MdcScope;
import com.splitwise.notification.messaging.NotificationMessage;
import com.splitwise.notification.persistence.entity.OutboxEventEntity;
import com.splitwise.notification.persistence.repository.OutboxRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxPublisherService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxPublisherService.class);
    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final NotificationQueueProperties queueProperties;
    private final NotificationOutboxProperties outboxProperties;
    private final ObjectMapper objectMapper;
    private final Counter outboxPublishedCounter;
    private final Counter outboxFailedCounter;

    public OutboxPublisherService(
            OutboxRepository outboxRepository,
            RabbitTemplate rabbitTemplate,
            NotificationQueueProperties queueProperties,
            NotificationOutboxProperties outboxProperties,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.queueProperties = queueProperties;
        this.outboxProperties = outboxProperties;
        this.objectMapper = objectMapper;
        this.outboxPublishedCounter = meterRegistry.counter("notification.outbox.published");
        this.outboxFailedCounter = meterRegistry.counter("notification.outbox.failed");
    }

    @Scheduled(fixedDelayString = "${notification.outbox.fixed-delay-ms:3000}")
    public void publishBatch() {
        List<OutboxEventEntity> unpublished = outboxRepository.findUnpublished(PageRequest.of(0, outboxProperties.getBatchSize()));
        if (!unpublished.isEmpty()) {
            LOGGER.info("Publishing outbox batch size={}", unpublished.size());
        }
        unpublished.forEach(this::publishSingle);
    }

    @Transactional
    public void publishSingle(OutboxEventEntity event) {
        try {
            NotificationMessage message = objectMapper.convertValue(event.getPayloadJson(), NotificationMessage.class);
            String correlationId = message.idempotencyKey();
            try (MdcScope ignored = MdcScope.with(Map.of(
                    LogContextKeys.CORRELATION_ID, correlationId,
                    LogContextKeys.NOTIFICATION_ID, message.notificationId().toString(),
                    LogContextKeys.CHANNEL, message.channel().name(),
                    LogContextKeys.EVENT_TYPE, message.eventType().name()
            ))) {
                CorrelationData correlationData = new CorrelationData(event.getId().toString());
                rabbitTemplate.convertAndSend(
                        queueProperties.getExchange(),
                        event.getRoutingKey(),
                        message,
                        outboundMessage -> {
                            outboundMessage.getMessageProperties().setHeader("X-Correlation-Id", correlationId);
                            outboundMessage.getMessageProperties().setHeader("notificationId", message.notificationId().toString());
                            outboundMessage.getMessageProperties().setHeader("eventType", message.eventType().name());
                            return outboundMessage;
                        },
                        correlationData
                );
                CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
                if (confirm != null && confirm.isAck()) {
                    outboxRepository.markPublished(event.getId(), Instant.now());
                    outboxPublishedCounter.increment();
                    LOGGER.info("Published outbox eventId={} routingKey={}", event.getId(), event.getRoutingKey());
                    return;
                }
                outboxFailedCounter.increment();
                LOGGER.error("Failed to publish outbox eventId={} due to broker nack", event.getId());
            }
        } catch (Exception exception) {
            outboxFailedCounter.increment();
            LOGGER.error("Failed to publish outbox eventId={}", event.getId(), exception);
        }
    }
}
