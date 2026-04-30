package com.splitwise.notification.service.email;

import com.splitwise.notification.config.CacheConfig;
import com.splitwise.notification.domain.NotificationEventType;
import com.splitwise.notification.exception.custom.email.PermanentEmailException;
import com.splitwise.notification.persistence.entity.EmailTemplateEntity;
import com.splitwise.notification.persistence.repository.EmailTemplateRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplateLookupService {
    private final EmailTemplateRepository templateRepository;

    public EmailTemplateLookupService(EmailTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Cacheable(cacheNames = CacheConfig.EMAIL_TEMPLATE_CACHE, key = "#eventType.name()")
    public EmailTemplateDefinition getTemplateDefinition(NotificationEventType eventType) {
        EmailTemplateEntity template = templateRepository.findByEventTypeAndActiveTrue(eventType)
                .or(() -> templateRepository.findByEventTypeAndActiveTrue(NotificationEventType.GENERIC))
                .orElseThrow(() -> new PermanentEmailException("No active email template found for event " + eventType));
        return new EmailTemplateDefinition(
                template.getSubjectTemplate(),
                template.getBodyTemplate(),
                template.isHtml()
        );
    }
}
