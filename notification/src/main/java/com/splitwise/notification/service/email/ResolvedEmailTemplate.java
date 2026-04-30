package com.splitwise.notification.service.email;

public record ResolvedEmailTemplate(
        String subject,
        String body,
        boolean html
) {
}
