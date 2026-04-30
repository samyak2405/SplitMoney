package com.splitwise.notification.persistence.repository;

import com.splitwise.notification.domain.NotificationChannel;
import com.splitwise.notification.persistence.entity.NotificationChannelStateEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationChannelStateRepository extends JpaRepository<NotificationChannelStateEntity, UUID> {
    Optional<NotificationChannelStateEntity> findByNotificationIdAndChannel(UUID notificationId, NotificationChannel channel);
}
