package com.splitwise.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "notification.routing")
public class NotificationRoutingProperties {
    private String inApp;
    private String email;

}
