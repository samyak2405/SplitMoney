package com.splitwise.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "notification.queue")
public class NotificationQueueProperties {
    private String exchange;
    private ChannelQueue inApp = new ChannelQueue();
    private ChannelQueue email = new ChannelQueue();

    @Setter
    @Getter
    public static class ChannelQueue {
        private String main;
        private String dlq;
        private String retry1m;
        private String retry5m;
        private String retry15m;
        private String retry60m;
    }
}
