package com.splitwise.notification.service.realtime;

import com.splitwise.notification.messaging.NotificationMessage;
import com.splitwise.notification.service.NotificationQueryService;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketRealtimeNotifier implements RealtimeNotifier {
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationQueryService notificationQueryService;

    public WebSocketRealtimeNotifier(
            SimpMessagingTemplate messagingTemplate,
            NotificationQueryService notificationQueryService
    ) {
        this.messagingTemplate = messagingTemplate;
        this.notificationQueryService = notificationQueryService;
    }

    @Override
    public void push(NotificationMessage message) {
        long unreadCount = notificationQueryService.unreadCount(message.userId());
        Map<String, Object> event = Map.of(
                "notificationId", message.notificationId().toString(),
                "eventType", message.eventType().name(),
                "payload", message.payload(),
                "unreadCount", unreadCount,
                "occurredAt", Instant.now().toString()
        );
        messagingTemplate.convertAndSend(
                "/topic/users/" + message.userId() + "/notifications",
                Objects.requireNonNull(event)
        );
    }
}
