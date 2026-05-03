package com.splitmoney.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TypingEvent {
    private String userId;
    private String email;
    private boolean typing;
}
