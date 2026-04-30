package com.splitwise.notification.service;

import com.splitwise.notification.domain.NotificationChannel;
import com.splitwise.notification.persistence.entity.NotificationPreferenceEntity;
import com.splitwise.notification.persistence.repository.NotificationPreferenceRepository;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {
    private final NotificationPreferenceRepository preferenceRepository;

    public Set<NotificationChannel> allowedChannels(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .map(this::toChannels)
                .orElse(EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL));
    }

    private Set<NotificationChannel> toChannels(NotificationPreferenceEntity preference) {
        Set<NotificationChannel> channels = EnumSet.noneOf(NotificationChannel.class);
        if (preference.isInappEnabled()) {
            channels.add(NotificationChannel.IN_APP);
        }
        if (preference.isEmailEnabled()) {
            channels.add(NotificationChannel.EMAIL);
        }
        return channels;
    }
}
