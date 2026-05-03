package com.splitmoney.chat.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
public class PresenceEvent {
    private String userId;
    private String email;
    private String status;   // ONLINE | OFFLINE
    private Set<String> onlineUserIds;
}
