package com.splitmoney.chat.messaging;

import com.splitmoney.chat.dto.ChatMessageEvent;
import com.splitmoney.chat.dto.MessageResponse;
import com.splitmoney.chat.observability.ChatMetrics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatKafkaConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMetrics chatMetrics;

    public ChatKafkaConsumer(SimpMessagingTemplate messagingTemplate, ChatMetrics chatMetrics) {
        this.messagingTemplate = messagingTemplate;
        this.chatMetrics = chatMetrics;
    }

    @KafkaListener(topics = "${chat.kafka.topic:chat.messages}", groupId = "chat-service")
    public void consume(ChatMessageEvent event) {
        String msgType = event.getMsgType() != null ? event.getMsgType() : "TEXT";
        chatMetrics.recordKafkaConsumed(msgType);
        chatMetrics.recordMessageE2eLatency(event.getCreatedAtEpochMs());

        MessageResponse response = MessageResponse.builder()
                .messageId(event.getMessageId())
                .groupId(event.getGroupId())
                .senderId(event.getSenderId())
                .senderEmail(event.getSenderEmail())
                .content(event.getContent())
                .msgType(event.getMsgType())
                .createdAtEpochMs(event.getCreatedAtEpochMs())
                .build();

        messagingTemplate.convertAndSend("/topic/chat/" + event.getGroupId(), response);
    }
}
