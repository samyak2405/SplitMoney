package com.splitwise.notification.messaging;

import com.splitwise.notification.config.NotificationQueueProperties;
import com.splitwise.notification.config.NotificationRetryProperties;
import com.splitwise.notification.messaging.policy.ChannelRoutingPolicy;
import com.splitwise.notification.messaging.policy.RetrySchedulePolicy;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RetryPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final NotificationQueueProperties queueProperties;
    private final NotificationRetryProperties retryProperties;
    private final ChannelRoutingPolicy channelRoutingPolicy;
    private final RetrySchedulePolicy retrySchedulePolicy;

    public RetryPublisher(
            RabbitTemplate rabbitTemplate,
            NotificationQueueProperties queueProperties,
            NotificationRetryProperties retryProperties,
            ChannelRoutingPolicy channelRoutingPolicy,
            RetrySchedulePolicy retrySchedulePolicy
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.queueProperties = queueProperties;
        this.retryProperties = retryProperties;
        this.channelRoutingPolicy = channelRoutingPolicy;
        this.retrySchedulePolicy = retrySchedulePolicy;
    }

    public boolean publishRetry(NotificationMessage message) {
        int currentAttempt = message.attempt();
        if (currentAttempt >= retryProperties.getMaxAttempts()) {
            return false;
        }
        String retryRoutingKey = retrySchedulePolicy.retryRoutingKeyFor(
                channelRoutingPolicy.queueFor(message.channel()),
                currentAttempt
        );
        NotificationMessage nextAttempt = new NotificationMessage(
                message.notificationId(),
                message.userId(),
                message.channel(),
                message.eventType(),
                message.idempotencyKey(),
                currentAttempt + 1,
                message.createdAt(),
                message.payload()
        );
        rabbitTemplate.convertAndSend(queueProperties.getExchange(), retryRoutingKey, nextAttempt);
        return true;
    }

    public void publishDlq(NotificationMessage message) {
        String dlqKey = channelRoutingPolicy.dlqKeyFor(message.channel());
        rabbitTemplate.convertAndSend(queueProperties.getExchange(), dlqKey, message);
    }

    public long delaySecondsForAttempt(int currentAttempt) {
        return retrySchedulePolicy.delaySecondsForAttempt(currentAttempt);
    }
}
