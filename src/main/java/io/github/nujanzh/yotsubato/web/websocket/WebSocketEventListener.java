package io.github.nujanzh.yotsubato.web.websocket;

import io.github.nujanzh.yotsubato.security.userdetails.AuthenticatedPrincipal;
import jakarta.annotation.Nullable;
import java.security.Principal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

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
        Principal eventUser = event.getUser();

        if (!(eventUser instanceof Authentication authentication)) {
            log.warn(
                    "Received SessionConnectedEvent without Spring Security Authentication."
                            + " sessionId={}, user={}",
                    sessionId,
                    eventUser);
            return;
        }

        if (!(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            log.warn(
                    "Received SessionConnectedEvent with unsupported principal type. sessionId={},"
                            + " principalType={}",
                    sessionId,
                    authentication.getPrincipal() == null
                            ? null
                            : authentication.getPrincipal().getClass().getName());
            return;
        }

        if (sessionId == null) {
            log.warn(
                    "Received SessionConnectedEvent without session ID. user={}",
                    principal.getName());
            return;
        }

        sessionRegistry.register(sessionId, principal.userId(), principal.expiresAt());
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
