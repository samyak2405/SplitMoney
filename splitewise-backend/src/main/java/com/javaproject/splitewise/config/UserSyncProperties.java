package com.javaproject.splitewise.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "user-sync")
public class UserSyncProperties {
    private String exchange = "auth.user.events";
    private String queue    = "splitmoney.user.sync";
    private String dlq      = "splitmoney.user.sync.dlq";
}
