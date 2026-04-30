package com.javaproject.application.service.impl;

import com.javaproject.application.dto.notification.NotificationEventMessage;
import com.javaproject.application.dto.notification.RegistrationOtpPayload;
import com.javaproject.application.enums.NotificationEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class NotificationEventPublisher {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";

    @Value("${app.notifications.service.base-url:http://localhost:8083}")
    private String notificationServiceBaseUrl;

    private final RestClient restClient = RestClient.builder().build();

    public void publishRegistrationOtpEvent(NotificationEventMessage message, String correlationId) {
        try {
            Map<String, Object> payload = buildPayloadMap(message.getPayload());

            Map<String, Object> body = new HashMap<>();
            body.put("userId", message.getUserId());
            body.put("eventType", message.getEventType().name());
            body.put("title", resolveTitle(message.getEventType()));
            body.put("body", resolveBody(message.getEventType(), message.getPayload()));
            body.put("payload", payload);

            restClient.post()
                    .uri(notificationServiceBaseUrl + "/internal/notifications/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(CORRELATION_HEADER, correlationId != null ? correlationId : "")
                    .header(IDEMPOTENCY_HEADER, message.getIdempotencyKey() != null ? message.getIdempotencyKey() : "")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Notification event sent. eventType={} userId={}", message.getEventType(), message.getUserId());
        } catch (Exception ex) {
            // Notification delivery must not fail the auth flow — log and continue.
            log.error("Failed to deliver notification event for userId={}. Error={}", message.getUserId(), ex.getMessage(), ex);
        }
    }

    private Map<String, Object> buildPayloadMap(RegistrationOtpPayload payload) {
        Map<String, Object> map = new HashMap<>();
        if (payload == null) return map;
        map.put("email", payload.getEmail());
        map.put("mobile", payload.getMobile());
        map.put("otp", payload.getOtp());
        map.put("expiryMinutes", payload.getExpiryMinutes());
        return map;
    }

    private String resolveTitle(NotificationEventType eventType) {
        return switch (eventType) {
            case REGISTRATION_OTP -> "Your Splitmoney OTP";
            case PASSWORD_RESET_OTP -> "Reset your Splitmoney password";
        };
    }

    private String resolveBody(NotificationEventType eventType, RegistrationOtpPayload payload) {
        String otp = payload != null ? payload.getOtp() : "";
        int expiry = payload != null ? payload.getExpiryMinutes() : 10;
        return switch (eventType) {
            case REGISTRATION_OTP -> "Your registration OTP is " + otp + ". It expires in " + expiry + " minutes.";
            case PASSWORD_RESET_OTP -> "Your password reset OTP is " + otp + ". It expires in " + expiry + " minutes.";
        };
    }
}
