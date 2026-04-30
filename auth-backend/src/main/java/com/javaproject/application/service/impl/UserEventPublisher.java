package com.javaproject.application.service.impl;

import com.javaproject.application.dto.event.UserEventMessage;
import com.javaproject.application.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventPublisher {

    public static final String EVENT_USER_REGISTERED = "user.registered";
    public static final String EVENT_USER_ACTIVATED  = "user.activated";

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.user-events.exchange:auth.user.events}")
    private String exchange;

    public void publishUserRegistered(User user) {
        publish(user, EVENT_USER_REGISTERED);
    }

    public void publishUserActivated(User user) {
        publish(user, EVENT_USER_ACTIVATED);
    }

    private void publish(User user, String routingKey) {
        try {
            UserEventMessage message = UserEventMessage.builder()
                    .eventId(UUID.randomUUID())
                    .eventType(routingKey)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .mobile(user.getMobile())
                    .active(user.isActive())
                    .occurredAt(OffsetDateTime.now())
                    .build();
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.info("user-event.published eventType={} userId={}", routingKey, user.getId());
        } catch (Exception ex) {
            // User sync is async and the consumer will handle retries.
            // Never let this block the auth flow.
            log.error("user-event.publish-failed eventType={} userId={} error={}", routingKey, user.getId(), ex.getMessage());
        }
    }
}
