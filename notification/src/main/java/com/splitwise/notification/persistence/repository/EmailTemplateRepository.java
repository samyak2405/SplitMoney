package com.splitwise.notification.persistence.repository;

import com.splitwise.notification.domain.NotificationEventType;
import com.splitwise.notification.persistence.entity.EmailTemplateEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplateEntity, UUID> {
    Optional<EmailTemplateEntity> findByEventTypeAndActiveTrue(NotificationEventType eventType);

    Optional<EmailTemplateEntity> findByEventType(NotificationEventType eventType);
}
