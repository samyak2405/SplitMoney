package com.splitwise.notification.service;

import com.splitwise.notification.persistence.entity.UserNotificationStatusEntity;
import com.splitwise.notification.persistence.repository.UserNotificationStatusRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationStatusCommandService {
    private final UserNotificationStatusRepository userStatusRepository;

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        userStatusRepository.findByUserIdAndNotificationId(userId, notificationId).ifPresent(status -> {
            status.setRead(true);
            status.setReadAt(Instant.now());
            status.setUpdatedAt(Instant.now());
            userStatusRepository.save(status);
        });
    }

    @Transactional
    public void markAllRead(UUID userId) {
        List<UserNotificationStatusEntity> unread = userStatusRepository.findByUserIdAndIsReadFalse(userId);
        Instant now = Instant.now();
        unread.forEach(status -> {
            status.setRead(true);
            status.setReadAt(now);
            status.setUpdatedAt(now);
        });
        userStatusRepository.saveAll(unread);
    }

    @Transactional
    public void markClicked(UUID userId, UUID notificationId) {
        userStatusRepository.findByUserIdAndNotificationId(userId, notificationId).ifPresent(status -> {
            status.setClicked(true);
            status.setClickedAt(Instant.now());
            status.setUpdatedAt(Instant.now());
            userStatusRepository.save(status);
        });
    }
}
