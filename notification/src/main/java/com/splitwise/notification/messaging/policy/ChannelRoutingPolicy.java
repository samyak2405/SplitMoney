package com.splitwise.notification.messaging.policy;

import com.splitwise.notification.config.NotificationQueueProperties;
import com.splitwise.notification.domain.NotificationChannel;

public interface ChannelRoutingPolicy {
    String routingKeyFor(NotificationChannel channel);

    String dlqKeyFor(NotificationChannel channel);

    NotificationQueueProperties.ChannelQueue queueFor(NotificationChannel channel);
}
