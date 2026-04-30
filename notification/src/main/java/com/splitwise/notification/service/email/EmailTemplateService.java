package com.splitwise.notification.service.email;

import com.splitwise.notification.domain.NotificationEventType;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");
    private final EmailTemplateLookupService templateLookupService;

    public EmailTemplateService(EmailTemplateLookupService templateLookupService) {
        this.templateLookupService = templateLookupService;
    }

    public ResolvedEmailTemplate resolve(NotificationEventType eventType, Map<String, Object> payload) {
        EmailTemplateDefinition template = templateLookupService.getTemplateDefinition(eventType);
        return new ResolvedEmailTemplate(
                render(template.subjectTemplate(), payload),
                render(template.bodyTemplate(), payload),
                template.html()
        );
    }

    private String render(String template, Map<String, Object> payload) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = payload.get(key);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
