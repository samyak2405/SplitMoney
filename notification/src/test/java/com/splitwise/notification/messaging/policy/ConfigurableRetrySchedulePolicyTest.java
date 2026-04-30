package com.splitwise.notification.messaging.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.splitwise.notification.config.NotificationQueueProperties;
import com.splitwise.notification.config.NotificationRetryProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigurableRetrySchedulePolicyTest {

    @Test
    void returnsConfiguredDefaultDelayWhenNoDelaysConfigured() {
        NotificationRetryProperties retryProperties = new NotificationRetryProperties();
        retryProperties.setDelaysSeconds(List.of());
        retryProperties.setDefaultDelaySeconds(42L);

        ConfigurableRetrySchedulePolicy policy = new ConfigurableRetrySchedulePolicy(retryProperties);

        assertThat(policy.delaySecondsForAttempt(3)).isEqualTo(42L);
    }

    @Test
    void returnsRetryRoutingKeyByAttemptWithUpperBound() {
        NotificationRetryProperties retryProperties = new NotificationRetryProperties();
        ConfigurableRetrySchedulePolicy policy = new ConfigurableRetrySchedulePolicy(retryProperties);
        NotificationQueueProperties.ChannelQueue queue = new NotificationQueueProperties.ChannelQueue();
        queue.setRetry1m("retry.1");
        queue.setRetry5m("retry.2");
        queue.setRetry15m("retry.3");
        queue.setRetry60m("retry.4");

        assertThat(policy.retryRoutingKeyFor(queue, 1)).isEqualTo("retry.1");
        assertThat(policy.retryRoutingKeyFor(queue, 2)).isEqualTo("retry.2");
        assertThat(policy.retryRoutingKeyFor(queue, 4)).isEqualTo("retry.4");
        assertThat(policy.retryRoutingKeyFor(queue, 9)).isEqualTo("retry.4");
    }
}
