package com.splitwise.notification.dto.response;

import com.splitwise.notification.dto.request.NotificationView;
import java.util.List;

public record NotificationListData(
        List<NotificationView> items,
        String nextCursor,
        long unreadCount
) {
}
