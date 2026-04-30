package com.javaproject.splitewise.service;

import com.javaproject.splitewise.messaging.UserSyncMessage;
import com.javaproject.splitewise.model.User;
import com.javaproject.splitewise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncService {

    private final UserRepository userRepository;

    @Transactional
    public void upsert(UserSyncMessage message) {
        OffsetDateTime now = OffsetDateTime.now();

        userRepository.findById(message.userId()).ifPresentOrElse(
                existing -> {
                    existing.setEmail(message.email());
                    existing.setPhoneNumber(message.mobile());
                    existing.setActive(message.active());
                    existing.setUpdatedAt(now);
                    userRepository.save(existing);
                    log.info("user-sync.updated userId={} eventType={}", message.userId(), message.eventType());
                },
                () -> {
                    User user = User.builder()
                            .id(message.userId())
                            .email(message.email())
                            .phoneNumber(message.mobile())
                            .isActive(message.active())
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    userRepository.save(user);
                    log.info("user-sync.created userId={} eventType={}", message.userId(), message.eventType());
                }
        );
    }
}
