package com.splitwise.notification.web;

import com.splitwise.notification.service.NotificationQueryService;
import com.splitwise.notification.service.NotificationStatusCommandService;
import com.splitwise.notification.dto.response.CursorPageResponse;
import com.splitwise.notification.dto.response.UnreadCountResponse;
import com.splitwise.notification.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class UserNotificationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserNotificationController.class);
    private final NotificationQueryService notificationQueryService;
    private final NotificationStatusCommandService notificationStatusCommandService;

    public UserNotificationController(
            NotificationQueryService notificationQueryService,
            NotificationStatusCommandService notificationStatusCommandService
    ) {
        this.notificationQueryService = notificationQueryService;
        this.notificationStatusCommandService = notificationStatusCommandService;
    }

    @GetMapping
    public CursorPageResponse list(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        UUID userId = getAuthenticatedUserId(httpRequest);
        LOGGER.info("List notifications userId={} cursorPresent={} limit={}", userId, cursor != null && !cursor.isBlank(), limit);
        return notificationQueryService.list(userId, cursor, Math.min(limit, 100));
    }

    @PostMapping("/{id}/read")
    public void markRead(HttpServletRequest httpRequest, @PathVariable("id") UUID notificationId) {
        UUID userId = getAuthenticatedUserId(httpRequest);
        notificationStatusCommandService.markRead(userId, notificationId);
        LOGGER.info("Marked notification as read userId={} notificationId={}", userId, notificationId);
    }

    @PostMapping("/read-all")
    public void markAllRead(HttpServletRequest httpRequest) {
        UUID userId = getAuthenticatedUserId(httpRequest);
        notificationStatusCommandService.markAllRead(userId);
        LOGGER.info("Marked all notifications as read userId={}", userId);
    }

    @PostMapping("/{id}/click")
    public void markClick(HttpServletRequest httpRequest, @PathVariable("id") UUID notificationId) {
        UUID userId = getAuthenticatedUserId(httpRequest);
        notificationStatusCommandService.markClicked(userId, notificationId);
        LOGGER.info("Marked notification as clicked userId={} notificationId={}", userId, notificationId);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(HttpServletRequest httpRequest) {
        UUID userId = getAuthenticatedUserId(httpRequest);
        LOGGER.info("Fetch unread count userId={}", userId);
        return new UnreadCountResponse(notificationQueryService.unreadCount(userId));
    }

    private UUID getAuthenticatedUserId(HttpServletRequest request) {
        Object value = request.getAttribute(JwtAuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        if (value instanceof UUID userId) {
            return userId;
        }
        throw new IllegalArgumentException("Authenticated user id is missing");
    }
}
