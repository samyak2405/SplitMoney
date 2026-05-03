package com.splitmoney.chat.dto;

import lombok.Data;

@Data
public class SendMessageRequest {
    private String content;
    private String msgType;
}
