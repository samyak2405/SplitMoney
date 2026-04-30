package com.splitwise.notification.persistence.repository;

import com.splitwise.notification.persistence.entity.UserNotificationStatusEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserNotificationStatusRepository extends JpaRepository<UserNotificationStatusEntity, UUID> {
    Optional<UserNotificationStatusEntity> findByUserIdAndNotificationId(UUID userId, UUID notificationId);

    List<UserNotificationStatusEntity> findByUserIdAndNotificationIdIn(UUID userId, List<UUID> notificationIds);

    List<UserNotificationStatusEntity> findByUserIdAndIsReadFalse(UUID userId);

    long countByUserIdAndIsReadFalse(UUID userId);
}
