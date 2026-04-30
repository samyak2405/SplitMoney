package com.splitmoney.application.infra.kafka;

import com.splitmoney.application.config.NotificationProperties;
import com.splitmoney.application.config.OutboxRelayProperties;
import com.splitmoney.application.domain.outbox.OutboxEvent;
import com.splitmoney.application.infra.persistence.OutboxEventRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxRelayWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxRelayWorker.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxRelayProperties relayProperties;
    private final NotificationProperties notificationProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelayWorker(
            OutboxEventRepository outboxEventRepository,
            OutboxRelayProperties relayProperties,
            NotificationProperties notificationProperties,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.relayProperties = relayProperties;
        this.notificationProperties = notificationProperties;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay.fixed-delay-ms:2000}")
    @Transactional
    public void relay() {
        List<OutboxEvent> batch = outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc(
                PageRequest.of(0, relayProperties.batchSize())
        );
        if (!batch.isEmpty()) {
            LOGGER.info("outbox relay picked batch size={}", batch.size());
        }
        for (OutboxEvent event : batch) {
            try {
                String topic = topicFor(event.getEventType());
                kafkaTemplate
                        .send(topic, event.getAggregateId().toString(), event.getPayload())
                        .get(10, TimeUnit.SECONDS);
                outboxEventRepository.markPublished(event.getId(), OffsetDateTime.now());
                LOGGER.info("outbox event published eventId={} topic={} aggregateId={} eventType={}",
                        event.getId(), topic, event.getAggregateId(), event.getEventType());
            } catch (Exception ex) {
                // Keep row unpublished so relay can safely retry.
                LOGGER.warn("Outbox publish failed for eventId={}, will retry. reason={}",
                        event.getId(), ex.getMessage());
            }
        }
    }

    private String topicFor(String eventType) {
        if (eventType.startsWith("payment.notification.")) {
            return notificationProperties.topic();
        }
        if (eventType.equals("payment.webhook_received")) {
            return "payment.webhook_received";
        }
        return "payment.lifecycle";
    }
}
