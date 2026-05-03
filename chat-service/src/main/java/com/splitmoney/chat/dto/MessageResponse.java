package com.splitmoney.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageResponse {
    private String messageId;
    private String groupId;
    private String senderId;
    private String senderEmail;
    private String content;
    private String msgType;
    private long createdAtEpochMs;
}
