package io.github.nujanzh.yotsubato.web.websocket;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionRegistry {
    private final ConcurrentHashMap<String, UUID> sessions = new ConcurrentHashMap<>();

    public void register(String sessionId, UUID userId) {
        Objects.requireNonNull(sessionId, "Session ID can't be null");
        Objects.requireNonNull(userId, "User ID can't be null");

        this.sessions.put(sessionId, userId);
    }

    public Optional<UUID> findUserId(String sessionId) {
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
