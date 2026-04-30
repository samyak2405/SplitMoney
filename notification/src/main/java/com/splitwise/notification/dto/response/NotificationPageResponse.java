package com.splitwise.notification.dto.response;

import com.splitwise.notification.dto.request.NotificationView;

import java.util.List;

public record NotificationPageResponse(
        List<NotificationView> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
