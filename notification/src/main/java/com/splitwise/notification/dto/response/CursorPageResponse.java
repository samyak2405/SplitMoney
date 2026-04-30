package com.splitwise.notification.dto.response;

import com.splitwise.notification.dto.request.NotificationView;
import java.util.List;

public record CursorPageResponse(
        List<NotificationView> items,
        String nextCursor
) {
}
