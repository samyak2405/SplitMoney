package com.splitwise.notification.messaging.policy;

import com.splitwise.notification.config.NotificationQueueProperties;
import com.splitwise.notification.config.NotificationRoutingProperties;
import com.splitwise.notification.domain.NotificationChannel;
import org.springframework.stereotype.Component;

@Component
public class PropertyBackedChannelRoutingPolicy implements ChannelRoutingPolicy {
    private final NotificationRoutingProperties routingProperties;
    private final NotificationQueueProperties queueProperties;

    public PropertyBackedChannelRoutingPolicy(
            NotificationRoutingProperties routingProperties,
            NotificationQueueProperties queueProperties
    ) {
        this.routingProperties = routingProperties;
        this.queueProperties = queueProperties;
    }

    @Override
    public String routingKeyFor(NotificationChannel channel) {
        return switch (channel) {
            case IN_APP -> routingProperties.getInApp();
            case EMAIL -> routingProperties.getEmail();
        };
    }

    @Override
    public String dlqKeyFor(NotificationChannel channel) {
        return queueFor(channel).getDlq();
    }

    @Override
    public NotificationQueueProperties.ChannelQueue queueFor(NotificationChannel channel) {
        return switch (channel) {
            case IN_APP -> queueProperties.getInApp();
            case EMAIL -> queueProperties.getEmail();
        };
    }
}
