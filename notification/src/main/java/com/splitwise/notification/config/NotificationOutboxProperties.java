package com.splitwise.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "notification.outbox")
public class NotificationOutboxProperties {
    private int batchSize = 100;
    private long fixedDelayMs = 3000L;

}
