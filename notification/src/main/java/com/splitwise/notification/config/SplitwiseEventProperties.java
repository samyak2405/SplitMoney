package com.splitwise.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "notification.splitwise")
public class SplitwiseEventProperties {
    private String exchange = "splitwise.events";
    private String queue = "notifications.splitwise.main";
    private String dlq = "notifications.splitwise.dlq";
    private String routingKeyGroupMemberAdded = "group.member.added";
    private String routingKeyGroupMemberRemoved = "group.member.removed";
    private String routingKeyExpenseAddedAgainstUser = "expense.added.against_user";
}
