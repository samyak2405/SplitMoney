package com.javaproject.splitewise.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "notification.queue")
public class NotificationQueueProperties {
    private String exchange = "splitwise.events";
    private String routingKey = "group.member.added";
}
