package com.splitwise.notification.web;

import com.splitwise.notification.dto.request.MarkReadRequest;
import com.splitwise.notification.dto.response.ApiEnvelope;
import com.splitwise.notification.dto.response.CursorPageResponse;
import com.splitwise.notification.dto.response.NotificationListData;
import com.splitwise.notification.dto.response.UnreadCountData;
import com.splitwise.notification.security.JwtAuthenticationFilter;
import com.splitwise.notification.service.NotificationQueryService;
import com.splitwise.notification.service.NotificationStatusCommandService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/v1")
@RequiredArgsConstructor
public class NotificationApiV1Controller {
    private static final String REQUEST_HEADER = "X-Request-Id";
    private final NotificationQueryService notificationQueryService;
    private final NotificationStatusCommandService notificationStatusCommandService;

    @GetMapping("/list")
    public ApiEnvelope<NotificationListData> list(
            HttpServletRequest httpRequest,
            @RequestHeader(value = REQUEST_HEADER, required = false) String requestId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "all") String status
    ) {
        UUID userId = getAuthenticatedUserId(httpRequest);
        CursorPageResponse response = notificationQueryService.list(userId, cursor, Math.min(limit, 100));
        long unreadCount = notificationQueryService.unreadCount(userId);

        return ApiEnvelope.<NotificationListData>builder()
                .requestId(requestIdOrGenerated(requestId))
                .success(true)
                .responseCode(String.valueOf(HttpStatus.OK.value()))
                .responseMessage("Notifications fetched successfully")
                .timestamp(OffsetDateTime.now())
                .data(new NotificationListData(response.items(), response.nextCursor(), unreadCount))
                .build();
    }

    @GetMapping("/unread-count")
    public ApiEnvelope<UnreadCountData> unreadCount(
            HttpServletRequest httpRequest,
            @RequestHeader(value = REQUEST_HEADER, required = false) String requestId
    ) {
        UUID userId = getAuthenticatedUserId(httpRequest);
        return ApiEnvelope.<UnreadCountData>builder()
                .requestId(requestIdOrGenerated(requestId))
                .success(true)
                .responseCode(String.valueOf(HttpStatus.OK.value()))
                .responseMessage("Unread count fetched successfully")
                .timestamp(OffsetDateTime.now())
                .data(new UnreadCountData(notificationQueryService.unreadCount(userId)))
                .build();
    }

    @PostMapping("/mark-read")
    public ApiEnvelope<Void> markRead(
            HttpServletRequest httpRequest,
            @RequestHeader(value = REQUEST_HEADER, required = false) String requestId,
            @Valid @RequestBody MarkReadRequest markReadRequest
    ) {
        UUID userId = getAuthenticatedUserId(httpRequest);
        markReadRequest.ids().forEach(notificationId -> notificationStatusCommandService.markRead(userId, notificationId));
        return ApiEnvelope.<Void>builder()
                .requestId(requestIdOrGenerated(requestId))
                .success(true)
                .responseCode(String.valueOf(HttpStatus.OK.value()))
                .responseMessage("Notifications marked as read")
                .timestamp(OffsetDateTime.now())
                .data(null)
                .build();
    }

    @PostMapping("/mark-all-read")
    public ApiEnvelope<Void> markAllRead(
            HttpServletRequest httpRequest,
            @RequestHeader(value = REQUEST_HEADER, required = false) String requestId
    ) {
        UUID userId = getAuthenticatedUserId(httpRequest);
        notificationStatusCommandService.markAllRead(userId);
        return ApiEnvelope.<Void>builder()
                .requestId(requestIdOrGenerated(requestId))
                .success(true)
                .responseCode(String.valueOf(HttpStatus.OK.value()))
                .responseMessage("All notifications marked as read")
                .timestamp(OffsetDateTime.now())
                .data(null)
                .build();
    }

    private String requestIdOrGenerated(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }

    private UUID getAuthenticatedUserId(HttpServletRequest request) {
        Object value = request.getAttribute(JwtAuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        if (value instanceof UUID userId) {
            return userId;
        }
        throw new IllegalArgumentException("Authenticated user id is missing");
    }
}
