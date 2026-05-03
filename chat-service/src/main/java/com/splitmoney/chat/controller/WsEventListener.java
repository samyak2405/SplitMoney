package com.splitmoney.chat.controller;

import com.splitmoney.chat.observability.ChatMetrics;
import com.splitmoney.chat.presence.PresenceService;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WsEventListener {

    private final PresenceService presenceService;
    private final ChatMetrics chatMetrics;

    public WsEventListener(PresenceService presenceService, ChatMetrics chatMetrics) {
        this.presenceService = presenceService;
        this.chatMetrics = chatMetrics;
    }

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        chatMetrics.wsConnected();
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        chatMetrics.wsDisconnected();

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        if (sessionAttrs == null) return;

        UUID userId = (UUID) sessionAttrs.get("wsUserId");
        // groupId stored at heartbeat time; mark offline for all groups by userId key only
        // (presence TTL handles group cleanup; explicit offline call clears key immediately)
        if (userId != null) {
            // We don't know the groupId here, but presence TTL will expire naturally.
            // If groupId was stored in session during heartbeat, use it.
            String groupId = (String) sessionAttrs.get("wsGroupId");
            if (groupId != null) {
                presenceService.setOffline(userId.toString(), groupId);
            }
        }
    }
}
