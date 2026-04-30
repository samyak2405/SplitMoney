package com.splitwise.notification.shared;

import com.splitwise.notification.messaging.NotificationMessage;
import org.springframework.stereotype.Component;

@Component
public class CorrelationIdResolver {

    public String resolve(String correlationIdHeader, NotificationMessage message) {
        if (correlationIdHeader == null || correlationIdHeader.isBlank()) {
            return message.idempotencyKey();
        }
        return correlationIdHeader;
    }
}
