package com.splitwise.notification.config;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "notification.retry")
public class NotificationRetryProperties {
    private int maxAttempts = 5;
    private List<Long> delaysSeconds = new ArrayList<>();
    private long defaultDelaySeconds = 60L;

}
