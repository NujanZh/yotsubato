package io.github.nujanzh.yotsubato.web.websocket;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Slf4j
@Component
public class WebSocketEventListener {

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        log.info("Connect attempt: user={}", getUserName(event.getUser()));
    }

    @EventListener
    public void handleConnected(SessionConnectedEvent event) {
        log.info("Connected: user={}", getUserName(event.getUser()));
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        log.info(
                "Disconnected: user={}, sessionId={}, closeStatus={}",
                getUserName(event.getUser()),
                event.getSessionId(),
                event.getCloseStatus());
    }

    private String getUserName(@Nullable Principal user) {
        return user != null ? user.getName() : "<anonymous>";
    }
}
