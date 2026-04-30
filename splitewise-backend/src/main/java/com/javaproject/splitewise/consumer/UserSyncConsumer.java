package com.javaproject.splitewise.consumer;

import com.javaproject.splitewise.messaging.UserSyncMessage;
import com.javaproject.splitewise.service.UserSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSyncConsumer {

    private final UserSyncService userSyncService;

    @RabbitListener(queues = "${user-sync.queue:splitmoney.user.sync}")
    public void consume(UserSyncMessage message) {
        log.info("user-sync.received eventType={} userId={}", message.eventType(), message.userId());
        userSyncService.upsert(message);
    }
}
