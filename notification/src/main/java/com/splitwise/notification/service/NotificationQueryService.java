package com.splitwise.notification.service;

import com.splitwise.notification.persistence.entity.NotificationEntity;
import com.splitwise.notification.persistence.entity.UserNotificationStatusEntity;
import com.splitwise.notification.persistence.repository.NotificationRepository;
import com.splitwise.notification.persistence.repository.UserNotificationStatusRepository;
import com.splitwise.notification.dto.response.CursorPageResponse;
import com.splitwise.notification.dto.request.NotificationView;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {
    private final NotificationRepository notificationRepository;
    private final UserNotificationStatusRepository userStatusRepository;

    @Transactional(readOnly = true)
    public CursorPageResponse list(UUID userId, String cursor, int size) {
        Cursor cursorData = decodeCursor(cursor);
        List<NotificationEntity> notifications = cursorData.createdAt() == null || cursorData.id() == null
                ? notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(
                        userId,
                        PageRequest.of(0, size + 1)
                )
                : notificationRepository.findByUserIdWithCursor(
                        userId,
                        cursorData.createdAt(),
                        cursorData.id(),
                        PageRequest.of(0, size + 1)
                );
        boolean hasMore = notifications.size() > size;
        List<NotificationEntity> pageItems = hasMore ? notifications.subList(0, size) : notifications;

        List<UUID> ids = pageItems.stream().map(NotificationEntity::getId).toList();
        Map<UUID, UserNotificationStatusEntity> statuses = userStatusRepository
                .findByUserIdAndNotificationIdIn(userId, ids)
                .stream()
                .collect(Collectors.toMap(UserNotificationStatusEntity::getNotificationId, Function.identity()));
        List<NotificationView> items = pageItems.stream()
                .map(notification -> toView(notification, statuses.get(notification.getId())))
                .toList();
        String nextCursor = hasMore && !pageItems.isEmpty()
                ? encodeCursor(pageItems.get(pageItems.size() - 1))
                : null;
        return new CursorPageResponse(items, nextCursor);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return userStatusRepository.countByUserIdAndIsReadFalse(userId);
    }

    private NotificationView toView(NotificationEntity notification, UserNotificationStatusEntity status) {
        boolean read = status != null && status.isRead();
        boolean clicked = status != null && status.isClicked();
        return new NotificationView(
                notification.getId(),
                notification.getEventType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getPayloadJson(),
                notification.getCreatedAt(),
                read,
                status == null ? null : status.getReadAt(),
                clicked,
                status == null ? null : status.getClickedAt()
        );
    }

    private String encodeCursor(NotificationEntity notification) {
        String value = notification.getCreatedAt().toString() + "|" + notification.getId();
        return Base64.getUrlEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private Cursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new Cursor(null, null);
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", 2);
            if (parts.length != 2) return new Cursor(null, null);
            return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (Exception ignored) {
            return new Cursor(null, null);
        }
    }

    private record Cursor(Instant createdAt, UUID id) {
    }
}
