package com.splitwise.notification.service.email;

public record EmailTemplateDefinition(
        String subjectTemplate,
        String bodyTemplate,
        boolean html
) {
}
