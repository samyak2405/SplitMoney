package com.splitmoney.chat.controller;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.splitmoney.chat.domain.Message;
import com.splitmoney.chat.dto.*;
import com.splitmoney.chat.messaging.ChatKafkaProducer;
import com.splitmoney.chat.observability.ChatMetrics;
import com.splitmoney.chat.presence.PresenceService;
import com.splitmoney.chat.repository.MessageRepository;
import com.splitmoney.chat.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat/v1")
public class ChatController {

    private static final int DEFAULT_LIMIT = 50;
    private static final DateTimeFormatter BUCKET_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneOffset.UTC);

    private final MessageRepository messageRepository;
    private final ChatKafkaProducer kafkaProducer;
    private final PresenceService presenceService;
    private final ChatMetrics chatMetrics;

    public ChatController(
            MessageRepository messageRepository,
            ChatKafkaProducer kafkaProducer,
            PresenceService presenceService,
            ChatMetrics chatMetrics
    ) {
        this.messageRepository = messageRepository;
        this.kafkaProducer = kafkaProducer;
        this.presenceService = presenceService;
        this.chatMetrics = chatMetrics;
    }

    // ── REST: message history ──────────────────────────────────────────────

    @GetMapping("/groups/{groupId}/messages")
    public ResponseEntity<ApiBody<Map<String, Object>>> getMessages(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String cursor,
            HttpServletRequest request
    ) {
        chatMetrics.recordHistoryFetch(cursor != null);

        int pageSize = Math.min(limit, 100);
        String bucket = BUCKET_FMT.format(Instant.now());

        List<Message> messages;
        if (cursor == null) {
            messages = messageRepository.findByGroupIdAndBucket(groupId, bucket, pageSize);
        } else {
            try {
                UUID cursorId = UUID.fromString(new String(Base64.getDecoder().decode(cursor)));
                messages = messageRepository.findByGroupIdAndBucketAndMessageIdBefore(
                        groupId, bucket, cursorId, pageSize);
            } catch (IllegalArgumentException e) {
                messages = messageRepository.findByGroupIdAndBucket(groupId, bucket, pageSize);
            }
        }

        List<MessageResponse> dtos = messages.stream()
                .map(m -> MessageResponse.builder()
                        .messageId(m.getMessageId().toString())
                        .groupId(m.getGroupId())
                        .senderId(m.getSenderId())
                        .senderEmail(m.getSenderEmail())
                        .content(m.getContent())
                        .msgType(m.getMsgType())
                        .createdAtEpochMs(m.getCreatedAt() != null ? m.getCreatedAt().toEpochMilli() : 0)
                        .build())
                .collect(Collectors.toList());

        String nextCursor = null;
        if (messages.size() == pageSize) {
            UUID lastId = messages.get(messages.size() - 1).getMessageId();
            nextCursor = Base64.getEncoder().encodeToString(lastId.toString().getBytes());
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("messages", dtos);
        data.put("nextCursor", nextCursor);

        return ResponseEntity.ok(ApiBody.<Map<String, Object>>builder()
                .success(true)
                .data(data)
                .build());
    }

    // ── REST: unread cursor ────────────────────────────────────────────────

    @GetMapping("/groups/{groupId}/unread-cursor")
    public ResponseEntity<ApiBody<Map<String, Object>>> getUnreadCursor(
            @PathVariable String groupId,
            HttpServletRequest request
    ) {
        UUID userId = (UUID) request.getAttribute(JwtAuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        Long lastSeenEpochMs = presenceService.getLastSeen(userId.toString(), groupId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lastSeenEpochMs", lastSeenEpochMs);
        return ResponseEntity.ok(ApiBody.<Map<String, Object>>builder()
                .success(true)
                .data(data)
                .build());
    }

    @PostMapping("/groups/{groupId}/mark-read")
    public ResponseEntity<ApiBody<Void>> markRead(
            @PathVariable String groupId,
            HttpServletRequest request
    ) {
        UUID userId = (UUID) request.getAttribute(JwtAuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        presenceService.markRead(userId.toString(), groupId);
        chatMetrics.recordMarkRead();
        return ResponseEntity.ok(ApiBody.<Void>builder().success(true).build());
    }

    // ── REST: presence ─────────────────────────────────────────────────────

    @GetMapping("/groups/{groupId}/presence")
    public ResponseEntity<ApiBody<Map<String, Object>>> getPresence(@PathVariable String groupId) {
        Set<String> online = presenceService.getOnlineMembers(groupId);
        Map<String, Object> data = Map.of("onlineUserIds", online);
        return ResponseEntity.ok(ApiBody.<Map<String, Object>>builder()
                .success(true)
                .data(data)
                .build());
    }

    // ── WebSocket: send message ────────────────────────────────────────────

    @MessageMapping("/chat/{groupId}/send")
    public void sendMessage(
            @DestinationVariable String groupId,
            SendMessageRequest req,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        if (sessionAttrs == null) return;

        UUID userId = (UUID) sessionAttrs.get("wsUserId");
        String email = (String) sessionAttrs.get("wsUserEmail");
        String wsToken = (String) sessionAttrs.get("wsToken");
        if (userId == null || req.getContent() == null || req.getContent().isBlank()) return;

        Instant now = Instant.now();
        UUID messageId = Uuids.timeBased(); // type-1 UUID required for timeuuid Cassandra column
        String bucket = BUCKET_FMT.format(now);

        Message msg = new Message();
        msg.setGroupId(groupId);
        msg.setBucket(bucket);
        msg.setMessageId(messageId);
        msg.setSenderId(userId.toString());
        msg.setSenderEmail(email != null ? email : userId.toString());
        msg.setContent(req.getContent().trim());
        String resolvedType = (req.getMsgType() != null
                && List.of("TEXT", "DOCUMENT").contains(req.getMsgType()))
                ? req.getMsgType() : "TEXT";
        msg.setMsgType(resolvedType);
        msg.setCreatedAt(now);

        messageRepository.save(msg);

        ChatMessageEvent event = ChatMessageEvent.builder()
                .messageId(messageId.toString())
                .groupId(groupId)
                .bucket(bucket)
                .senderId(msg.getSenderId())
                .senderEmail(msg.getSenderEmail())
                .content(msg.getContent())
                .msgType(msg.getMsgType())
                .createdAtEpochMs(now.toEpochMilli())
                .userToken(wsToken)
                .build();

        kafkaProducer.publish(event);
    }

    // ── WebSocket: typing indicator ────────────────────────────────────────

    @MessageMapping("/chat/{groupId}/typing")
    public void typing(
            @DestinationVariable String groupId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        if (sessionAttrs == null) return;

        UUID userId = (UUID) sessionAttrs.get("wsUserId");
        String email = (String) sessionAttrs.get("wsUserEmail");
        if (userId == null) return;

        presenceService.setTyping(userId.toString(), email != null ? email : userId.toString(), groupId);
    }

    // ── WebSocket: presence heartbeat ─────────────────────────────────────

    @MessageMapping("/chat/{groupId}/heartbeat")
    public void heartbeat(
            @DestinationVariable String groupId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        if (sessionAttrs == null) return;

        UUID userId = (UUID) sessionAttrs.get("wsUserId");
        String email = (String) sessionAttrs.get("wsUserEmail");
        if (userId == null) return;

        chatMetrics.recordHeartbeat();
        presenceService.heartbeat(userId.toString(), email != null ? email : userId.toString(), groupId);
    }
}
