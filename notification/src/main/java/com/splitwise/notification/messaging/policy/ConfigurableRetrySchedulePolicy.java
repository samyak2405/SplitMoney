package com.splitwise.notification.messaging.policy;

import com.splitwise.notification.config.NotificationQueueProperties;
import com.splitwise.notification.config.NotificationRetryProperties;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ConfigurableRetrySchedulePolicy implements RetrySchedulePolicy {
    private final NotificationRetryProperties retryProperties;

    public ConfigurableRetrySchedulePolicy(NotificationRetryProperties retryProperties) {
        this.retryProperties = retryProperties;
    }

    @Override
    public long delaySecondsForAttempt(int currentAttempt) {
        List<Long> delays = retryProperties.getDelaysSeconds();
        if (delays.isEmpty()) {
            return retryProperties.getDefaultDelaySeconds();
        }
        int index = Math.max(0, Math.min(currentAttempt - 1, delays.size() - 1));
        return delays.get(index);
    }

    @Override
    public String retryRoutingKeyFor(NotificationQueueProperties.ChannelQueue queue, int currentAttempt) {
        List<String> retryRoutingKeys = List.of(
                queue.getRetry1m(),
                queue.getRetry5m(),
                queue.getRetry15m(),
                queue.getRetry60m()
        );
        int index = Math.max(0, Math.min(currentAttempt - 1, retryRoutingKeys.size() - 1));
        return retryRoutingKeys.get(index);
    }
}
