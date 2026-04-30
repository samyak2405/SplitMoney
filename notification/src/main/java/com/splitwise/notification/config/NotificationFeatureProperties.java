package com.splitwise.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "notification.features")
public class NotificationFeatureProperties {
    @Setter
    private boolean inAppEnabled = true;
    @Setter
    private boolean emailEnabled = false;
    private int emailCanaryPercent = 0;

    public void setEmailCanaryPercent(int emailCanaryPercent) {
        this.emailCanaryPercent = Math.max(0, Math.min(emailCanaryPercent, 100));
    }
}
