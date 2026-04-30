package com.splitwise.notification.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.splitwise.notification.config.NotificationFeatureProperties;
import com.splitwise.notification.domain.NotificationChannel;
import com.splitwise.notification.persistence.repository.NotificationPreferenceRepository;
import com.splitwise.notification.service.NotificationPreferenceService;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChannelSelectionServiceTest {

    @Test
    void keepsBothChannelsWhenAllowedAndEnabled() {
        UUID userId = UUID.randomUUID();
        NotificationFeatureProperties featureProperties = defaultFeatures();
        NotificationPreferenceService preferenceService = staticPreferenceService(EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL));
        ChannelSelectionService channelSelectionService = new ChannelSelectionService(preferenceService, featureProperties);

        Set<NotificationChannel> channels = channelSelectionService.select(userId);

        assertThat(channels).containsExactlyInAnyOrder(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
    }

    @Test
    void removesEmailWhenFeatureFlagDisabled() {
        UUID userId = UUID.randomUUID();
        NotificationFeatureProperties featureProperties = defaultFeatures();
        NotificationPreferenceService preferenceService = staticPreferenceService(EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL));
        featureProperties.setEmailEnabled(false);
        ChannelSelectionService channelSelectionService = new ChannelSelectionService(preferenceService, featureProperties);

        Set<NotificationChannel> channels = channelSelectionService.select(userId);

        assertThat(channels).containsExactly(NotificationChannel.IN_APP);
    }

    @Test
    void removesEmailWhenCanaryPercentZero() {
        UUID userId = UUID.randomUUID();
        NotificationFeatureProperties featureProperties = defaultFeatures();
        NotificationPreferenceService preferenceService = staticPreferenceService(EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL));
        featureProperties.setEmailEnabled(true);
        featureProperties.setEmailCanaryPercent(0);
        ChannelSelectionService channelSelectionService = new ChannelSelectionService(preferenceService, featureProperties);

        Set<NotificationChannel> channels = channelSelectionService.select(userId);

        assertThat(channels).containsExactly(NotificationChannel.IN_APP);
    }

    private NotificationFeatureProperties defaultFeatures() {
        NotificationFeatureProperties featureProperties = new NotificationFeatureProperties();
        featureProperties.setInAppEnabled(true);
        featureProperties.setEmailEnabled(true);
        featureProperties.setEmailCanaryPercent(100);
        return featureProperties;
    }

    private NotificationPreferenceService staticPreferenceService(Set<NotificationChannel> channels) {
        return new NotificationPreferenceService((NotificationPreferenceRepository) null) {
            @Override
            public Set<NotificationChannel> allowedChannels(UUID userId) {
                return channels;
            }
        };
    }
}
