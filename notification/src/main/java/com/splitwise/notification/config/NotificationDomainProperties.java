package com.splitwise.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "notification.domain")
public class NotificationDomainProperties {
    private String defaultTenant = "default";
    private String outboxAggregateType = "notification";
    private String outboxEventType = "NotificationMessage";

}
