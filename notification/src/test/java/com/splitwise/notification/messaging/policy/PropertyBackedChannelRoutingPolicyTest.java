package com.splitwise.notification.messaging.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.splitwise.notification.config.NotificationQueueProperties;
import com.splitwise.notification.config.NotificationRoutingProperties;
import com.splitwise.notification.domain.NotificationChannel;
import org.junit.jupiter.api.Test;

class PropertyBackedChannelRoutingPolicyTest {

    @Test
    void resolvesRoutingAndDlqKeysFromProperties() {
        NotificationRoutingProperties routingProperties = new NotificationRoutingProperties();
        routingProperties.setInApp("in.app.route");
        routingProperties.setEmail("email.route");

        NotificationQueueProperties queueProperties = new NotificationQueueProperties();
        queueProperties.getInApp().setDlq("in.app.dlq");
        queueProperties.getEmail().setDlq("email.dlq");

        PropertyBackedChannelRoutingPolicy policy = new PropertyBackedChannelRoutingPolicy(routingProperties, queueProperties);

        assertThat(policy.routingKeyFor(NotificationChannel.IN_APP)).isEqualTo("in.app.route");
        assertThat(policy.routingKeyFor(NotificationChannel.EMAIL)).isEqualTo("email.route");
        assertThat(policy.dlqKeyFor(NotificationChannel.IN_APP)).isEqualTo("in.app.dlq");
        assertThat(policy.dlqKeyFor(NotificationChannel.EMAIL)).isEqualTo("email.dlq");
    }
}
