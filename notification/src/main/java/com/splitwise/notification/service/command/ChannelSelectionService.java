package com.splitwise.notification.service.command;

import com.splitwise.notification.config.NotificationFeatureProperties;
import com.splitwise.notification.domain.NotificationChannel;
import com.splitwise.notification.service.NotificationPreferenceService;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ChannelSelectionService {
    private final NotificationPreferenceService preferenceService;
    private final NotificationFeatureProperties featureProperties;

    public ChannelSelectionService(
            NotificationPreferenceService preferenceService,
            NotificationFeatureProperties featureProperties
    ) {
        this.preferenceService = preferenceService;
        this.featureProperties = featureProperties;
    }

    public Set<NotificationChannel> select(UUID userId) {
        Set<NotificationChannel> channels = preferenceService.allowedChannels(userId);
        if (channels.isEmpty()) {
            return java.util.EnumSet.noneOf(NotificationChannel.class);
        }
        Set<NotificationChannel> filtered = java.util.EnumSet.copyOf(channels);
        if (!featureProperties.isInAppEnabled()) {
            filtered.remove(NotificationChannel.IN_APP);
        }
        if (!featureProperties.isEmailEnabled() || !isCanaryUser(userId, featureProperties.getEmailCanaryPercent())) {
            filtered.remove(NotificationChannel.EMAIL);
        }
        return filtered;
    }

    private boolean isCanaryUser(UUID userId, int canaryPercent) {
        if (canaryPercent >= 100) {
            return true;
        }
        if (canaryPercent <= 0) {
            return false;
        }
        int bucket = Math.floorMod(userId.hashCode(), 100);
        return bucket < canaryPercent;
    }
}
