package com.splitmoney.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEvent {
    private String messageId;
    private String groupId;
    private String bucket;
    private String senderId;
    private String senderEmail;
    private String content;
    private String msgType;
    private long createdAtEpochMs;
    private String userToken;
}
