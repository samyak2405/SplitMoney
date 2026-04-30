package com.splitwise.notification.service.command;

import com.splitwise.notification.config.NotificationDomainProperties;
import com.splitwise.notification.messaging.NotificationMessage;
import com.splitwise.notification.messaging.policy.ChannelRoutingPolicy;
import com.splitwise.notification.persistence.entity.OutboxEventEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventFactory {
    private final NotificationDomainProperties domainProperties;
    private final ChannelRoutingPolicy channelRoutingPolicy;
    private final NotificationMessageMapper messageMapper;

    public OutboxEventFactory(
            NotificationDomainProperties domainProperties,
            ChannelRoutingPolicy channelRoutingPolicy,
            NotificationMessageMapper messageMapper
    ) {
        this.domainProperties = domainProperties;
        this.channelRoutingPolicy = channelRoutingPolicy;
        this.messageMapper = messageMapper;
    }

    public OutboxEventEntity create(UUID aggregateId, NotificationMessage message, Instant now) {
        OutboxEventEntity outbox = new OutboxEventEntity();
        outbox.setId(UUID.randomUUID());
        outbox.setAggregateType(domainProperties.getOutboxAggregateType());
        outbox.setAggregateId(aggregateId);
        outbox.setEventType(domainProperties.getOutboxEventType());
        outbox.setRoutingKey(channelRoutingPolicy.routingKeyFor(message.channel()));
        outbox.setPayloadJson(messageMapper.outboxPayload(message));
        outbox.setCreatedAt(now);
        return outbox;
    }
}
