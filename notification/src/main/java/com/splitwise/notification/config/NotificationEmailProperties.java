package com.splitwise.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "notification.email")
public class NotificationEmailProperties {
    private String provider = "gmail";
    private String fromAddress;
}
