package com.splitwise.notification.service.email;

import com.splitwise.notification.config.CacheConfig;
import com.splitwise.notification.domain.NotificationEventType;
import com.splitwise.notification.persistence.entity.EmailTemplateEntity;
import com.splitwise.notification.persistence.repository.EmailTemplateRepository;
import com.splitwise.notification.dto.request.EmailTemplateUpsertRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailTemplateAdminService {
    private final EmailTemplateRepository templateRepository;

    public EmailTemplateAdminService(EmailTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.EMAIL_TEMPLATE_CACHE, key = "#request.eventType().name()")
    public void upsertTemplate(EmailTemplateUpsertRequest request) {
        EmailTemplateEntity entity = templateRepository.findByEventType(request.eventType())
                .orElseGet(EmailTemplateEntity::new);
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
            entity.setEventType(request.eventType());
        }
        entity.setSubjectTemplate(request.subjectTemplate());
        entity.setBodyTemplate(request.bodyTemplate());
        entity.setHtml(request.html());
        entity.setActive(request.active());
        entity.setUpdatedAt(Instant.now());
        templateRepository.save(entity);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.EMAIL_TEMPLATE_CACHE, key = "#eventType.name()")
    public void activate(NotificationEventType eventType) {
        EmailTemplateEntity entity = templateRepository.findByEventType(eventType)
                .orElseThrow(() -> new IllegalArgumentException("Template not found for event " + eventType));
        entity.setActive(true);
        entity.setUpdatedAt(Instant.now());
        templateRepository.save(entity);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.EMAIL_TEMPLATE_CACHE, key = "#eventType.name()")
    public void deactivate(NotificationEventType eventType) {
        EmailTemplateEntity entity = templateRepository.findByEventType(eventType)
                .orElseThrow(() -> new IllegalArgumentException("Template not found for event " + eventType));
        entity.setActive(false);
        entity.setUpdatedAt(Instant.now());
        templateRepository.save(entity);
    }

    @CacheEvict(cacheNames = CacheConfig.EMAIL_TEMPLATE_CACHE, allEntries = true)
    public void evictAll() {
        // Hook for bulk refresh after script-based template updates.
    }
}
