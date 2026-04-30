package com.splitwise.notification.web;

import com.splitwise.notification.service.NotificationCommandService;
import com.splitwise.notification.dto.response.CreateNotificationResponse;
import com.splitwise.notification.dto.request.InternalNotificationEventRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalNotificationController.class);
    private final NotificationCommandService notificationCommandService;

    public InternalNotificationController(NotificationCommandService notificationCommandService) {
        this.notificationCommandService = notificationCommandService;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CreateNotificationResponse create(
            @Valid @RequestBody InternalNotificationEventRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        String key = idempotencyKey == null || idempotencyKey.isBlank()
                ? UUID.randomUUID().toString()
                : idempotencyKey;
        UUID notificationId = notificationCommandService.createNotification(request, key);
        LOGGER.info("Created notification notificationId={} eventType={} userId={}", notificationId, request.eventType(), request.userId());
        return new CreateNotificationResponse(notificationId);
    }
}
