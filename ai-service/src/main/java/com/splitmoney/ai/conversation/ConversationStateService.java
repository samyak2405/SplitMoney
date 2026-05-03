package com.splitmoney.ai.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationStateService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${ai.conversation.ttl-minutes:30}")
    private int ttlMinutes;

    public Optional<ConversationState> load(String groupId, String userId) {
        String value = redis.opsForValue().get(key(groupId, userId));
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, ConversationState.class));
        } catch (Exception e) {
            log.warn("Failed to deserialize conversation state for group={} user={}", groupId, userId, e);
            return Optional.empty();
        }
    }

    public void save(ConversationState state) {
        try {
            String json = objectMapper.writeValueAsString(state);
            redis.opsForValue().set(key(state.getGroupId(), state.getUserId()),
                    json, Duration.ofMinutes(ttlMinutes));
        } catch (Exception e) {
            log.error("Failed to save conversation state group={} user={}", state.getGroupId(), state.getUserId(), e);
        }
    }

    public void delete(String groupId, String userId) {
        redis.delete(key(groupId, userId));
    }

    private String key(String groupId, String userId) {
        return "splity:conv:" + groupId + ":" + userId;
    }
}
