package io.github.nujanzh.yotsubato.web.websocket;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class WebSocketSessionRegistry {
    private final ConcurrentHashMap<String, WebSocketSessionInfo> sessions =
            new ConcurrentHashMap<>();

    public void register(String sessionId, UUID userId, Instant expiresAt) {
        Objects.requireNonNull(sessionId, "Session ID can't be null");

        WebSocketSessionInfo sessionInfo = new WebSocketSessionInfo(userId, expiresAt);
        this.sessions.put(sessionId, sessionInfo);
    }

    public Optional<WebSocketSessionInfo> findBySessionId(String sessionId) {
        if (sessionId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(this.sessions.get(sessionId));
    }

    public void unregister(String sessionId) {
        if (sessionId != null) {
            this.sessions.remove(sessionId);
        }
    }
}
