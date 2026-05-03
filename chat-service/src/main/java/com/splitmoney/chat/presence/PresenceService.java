package com.splitmoney.chat.presence;

import com.splitmoney.chat.dto.PresenceEvent;
import com.splitmoney.chat.dto.TypingEvent;
import com.splitmoney.chat.observability.ChatMetrics;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class PresenceService {

    private static final String PRESENCE_KEY = "presence:";
    private static final String TYPING_KEY = "typing:";
    private static final String GROUP_MEMBERS_KEY = "chat:members:";
    private static final String LAST_SEEN_KEY = "chat:last_seen:";

    private final StringRedisTemplate redis;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMetrics chatMetrics;
    private final long presenceTtl;
    private final long typingTtl;

    public PresenceService(
            StringRedisTemplate redis,
            SimpMessagingTemplate messagingTemplate,
            ChatMetrics chatMetrics,
            @Value("${chat.presence.ttl-seconds:30}") long presenceTtl,
            @Value("${chat.typing.ttl-seconds:3}") long typingTtl
    ) {
        this.redis = redis;
        this.messagingTemplate = messagingTemplate;
        this.chatMetrics = chatMetrics;
        this.presenceTtl = presenceTtl;
        this.typingTtl = typingTtl;
    }

    public void heartbeat(String userId, String email, String groupId) {
        String presenceKey = PRESENCE_KEY + userId;
        boolean wasAbsent = Boolean.FALSE.equals(redis.hasKey(presenceKey));
        redis.opsForValue().set(presenceKey, email, Duration.ofSeconds(presenceTtl));
        redis.opsForSet().add(GROUP_MEMBERS_KEY + groupId, userId);
        redis.expire(GROUP_MEMBERS_KEY + groupId, Duration.ofSeconds(presenceTtl + 10));

        if (wasAbsent) {
            // First heartbeat after absence — user came online
            chatMetrics.recordPresenceTransition("ONLINE");
            Set<String> online = getOnlineMembers(groupId);
            messagingTemplate.convertAndSend("/topic/chat/" + groupId + "/presence",
                    PresenceEvent.builder()
                            .userId(userId)
                            .email(email)
                            .status("ONLINE")
                            .onlineUserIds(online)
                            .build());
        }
    }

    public void setOffline(String userId, String groupId) {
        redis.delete(PRESENCE_KEY + userId);
        redis.opsForSet().remove(GROUP_MEMBERS_KEY + groupId, userId);
        chatMetrics.recordPresenceTransition("OFFLINE");

        Set<String> online = getOnlineMembers(groupId);
        messagingTemplate.convertAndSend("/topic/chat/" + groupId + "/presence",
                PresenceEvent.builder()
                        .userId(userId)
                        .status("OFFLINE")
                        .onlineUserIds(online)
                        .build());
    }

    public Set<String> getOnlineMembers(String groupId) {
        Set<String> members = redis.opsForSet().members(GROUP_MEMBERS_KEY + groupId);
        if (members == null) return Set.of();
        return members.stream()
                .filter(uid -> Boolean.TRUE.equals(redis.hasKey(PRESENCE_KEY + uid)))
                .collect(Collectors.toSet());
    }

    public Long getLastSeen(String userId, String groupId) {
        String val = redis.opsForValue().get(LAST_SEEN_KEY + userId + ":" + groupId);
        return val != null ? Long.parseLong(val) : null;
    }

    public void markRead(String userId, String groupId) {
        redis.opsForValue().set(LAST_SEEN_KEY + userId + ":" + groupId,
                String.valueOf(System.currentTimeMillis()));
    }

    public void setTyping(String userId, String email, String groupId) {
        String key = TYPING_KEY + groupId + ":" + userId;
        boolean wasAbsent = Boolean.FALSE.equals(redis.hasKey(key));
        redis.opsForValue().set(key, email, Duration.ofSeconds(typingTtl));

        if (wasAbsent) {
            // Count unique typing starts (one per TTL window, not every frame)
            chatMetrics.recordTypingEvent();
            messagingTemplate.convertAndSend("/topic/chat/" + groupId + "/typing",
                    TypingEvent.builder()
                            .userId(userId)
                            .email(email)
                            .typing(true)
                            .build());
        }
    }
}
