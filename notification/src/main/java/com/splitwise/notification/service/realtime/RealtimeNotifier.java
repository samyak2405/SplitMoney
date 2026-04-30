package com.splitwise.notification.service.realtime;

import com.splitwise.notification.messaging.NotificationMessage;

public interface RealtimeNotifier {
    void push(NotificationMessage message);
}
