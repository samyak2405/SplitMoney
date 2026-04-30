package com.splitwise.notification.messaging.policy;

import com.splitwise.notification.config.NotificationQueueProperties;

public interface RetrySchedulePolicy {
    long delaySecondsForAttempt(int currentAttempt);

    String retryRoutingKeyFor(NotificationQueueProperties.ChannelQueue queue, int currentAttempt);
}
