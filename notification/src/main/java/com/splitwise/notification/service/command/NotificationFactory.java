package com.splitwise.notification.service.command;

import com.splitwise.notification.domain.DeliveryState;
import com.splitwise.notification.domain.NotificationChannel;
import com.splitwise.notification.persistence.entity.NotificationChannelStateEntity;
import com.splitwise.notification.persistence.entity.NotificationEntity;
import com.splitwise.notification.persistence.entity.UserNotificationStatusEntity;
import com.splitwise.notification.dto.request.InternalNotificationEventRequest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NotificationFactory {

    public NotificationEntity notification(
            InternalNotificationEventRequest request,
            UUID notificationId,
            Instant now,
            String tenantId,
            Map<String, Object> payload
    ) {
        NotificationEntity notification = new NotificationEntity();
        notification.setId(notificationId);
        notification.setTenantId(tenantId);
        notification.setUserId(request.userId());
        notification.setEventType(request.eventType());
        notification.setTitle(request.title());
        notification.setBody(request.body());
        notification.setPayloadJson(payload);
        notification.setCreatedAt(now);
        return notification;
    }

    public UserNotificationStatusEntity userStatus(UUID userId, UUID notificationId, Instant now) {
        UserNotificationStatusEntity userStatus = new UserNotificationStatusEntity();
        userStatus.setId(UUID.randomUUID());
        userStatus.setUserId(userId);
        userStatus.setNotificationId(notificationId);
        userStatus.setRead(false);
        userStatus.setClicked(false);
        userStatus.setUpdatedAt(now);
        return userStatus;
    }

    public NotificationChannelStateEntity channelState(UUID notificationId, NotificationChannel channel, Instant now) {
        NotificationChannelStateEntity state = new NotificationChannelStateEntity();
        state.setId(UUID.randomUUID());
        state.setNotificationId(notificationId);
        state.setChannel(channel);
        state.setState(DeliveryState.PENDING);
        state.setAttempts(0);
        state.setUpdatedAt(now);
        return state;
    }
}
