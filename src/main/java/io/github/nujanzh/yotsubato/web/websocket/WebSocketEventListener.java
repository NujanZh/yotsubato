package io.github.nujanzh.yotsubato.web.websocket;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@Component
public class WebSocketEventListener {

    private final WebSocketSessionRegistry sessionRegistry;

    public WebSocketEventListener(WebSocketSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        log.info("Connect attempt: user={}", getUserName(event.getUser()));
    }

    @EventListener
    public void handleConnected(SessionConnectedEvent event) {
        Message<byte[]> message = event.getMessage();
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
        String sessionId = headers.getSessionId();
        Principal principal = event.getUser();

        if (principal == null) {
            log.warn(
                    "Received SessionConnectedEvent without authenticated principal. sessionId={}",
                    sessionId);
            return;
        }

        if (sessionId == null) {
            log.warn(
                    "Received SessionConnectedEvent without session ID. user={}",
                    principal.getName());
            return;
        }

        UUID userId;

        try {
            userId = UUID.fromString(principal.getName());
        } catch (IllegalArgumentException ex) {
            log.warn("Principal name is not a valid UUID: {}", principal.getName());
            return;
        }

        sessionRegistry.register(sessionId, userId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        log.info(
                "Disconnected: user={}, sessionId={}, closeStatus={}",
                getUserName(event.getUser()),
                event.getSessionId(),
                event.getCloseStatus());

        sessionRegistry.unregister(event.getSessionId());
    }

    private String getUserName(@Nullable Principal user) {
        return user != null ? user.getName() : "<anonymous>";
    }
}
