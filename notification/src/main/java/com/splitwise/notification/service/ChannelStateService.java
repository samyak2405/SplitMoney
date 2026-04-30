package com.splitwise.notification.service;

import com.splitwise.notification.domain.DeliveryState;
import com.splitwise.notification.domain.NotificationChannel;
import com.splitwise.notification.persistence.entity.NotificationChannelStateEntity;
import com.splitwise.notification.persistence.repository.NotificationChannelStateRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelStateService {
    private final NotificationChannelStateRepository channelStateRepository;

    public ChannelStateService(NotificationChannelStateRepository channelStateRepository) {
        this.channelStateRepository = channelStateRepository;
    }

    @Transactional
    public boolean markSent(UUID notificationId, NotificationChannel channel) {
        return channelStateRepository.findByNotificationIdAndChannel(notificationId, channel)
                .map(state -> {
                    if (state.getState() == DeliveryState.SENT) {
                        return false;
                    }
                    state.setState(DeliveryState.SENT);
                    state.setUpdatedAt(Instant.now());
                    state.setLastError(null);
                    channelStateRepository.save(state);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public int markRetrying(UUID notificationId, NotificationChannel channel, String error, Instant nextRetryAt) {
        return channelStateRepository.findByNotificationIdAndChannel(notificationId, channel)
                .map(state -> {
                    int attempts = state.getAttempts() + 1;
                    state.setAttempts(attempts);
                    state.setState(DeliveryState.RETRYING);
                    state.setLastError(error);
                    state.setNextRetryAt(nextRetryAt);
                    state.setUpdatedAt(Instant.now());
                    channelStateRepository.save(state);
                    return attempts;
                })
                .orElse(0);
    }

    @Transactional
    public void markFailed(UUID notificationId, NotificationChannel channel, String error) {
        channelStateRepository.findByNotificationIdAndChannel(notificationId, channel)
                .ifPresent(state -> {
                    state.setAttempts(state.getAttempts() + 1);
                    state.setState(DeliveryState.FAILED);
                    state.setLastError(error);
                    state.setUpdatedAt(Instant.now());
                    channelStateRepository.save(state);
                });
    }
}
