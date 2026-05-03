package com.splitmoney.ai.cassandra;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.splitmoney.ai.conversation.ConversationState.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SplityMessageService {

    private final SplityMessageRepository repository;

    public SplityMessage saveMessage(String groupId, String userId, String role, String content) {
        SplityMessage msg = SplityMessage.builder()
                .groupId(groupId)
                .userId(userId)
                .messageId(Uuids.timeBased())
                .role(role)
                .content(content)
                .createdAt(Instant.now())
                .build();
        return repository.save(msg);
    }

    public List<SplityMessage> loadRawHistory(String groupId, String userId, int limit) {
        List<SplityMessage> desc = repository.findHistory(groupId, userId, limit);
        List<SplityMessage> asc = new ArrayList<>(desc);
        Collections.reverse(asc);
        return asc;
    }

    public List<ChatMessage> loadHistoryForAi(String groupId, String userId) {
        return loadRawHistory(groupId, userId, 50).stream()
                .map(m -> new ChatMessage(m.getRole(), m.getContent()))
                .toList();
    }
}
