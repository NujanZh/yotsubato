package io.github.nujanzh.yotsubato.web.websocket;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record WebSocketSessionInfo(UUID userId, Instant expiresAt) {

    public WebSocketSessionInfo {
        Objects.requireNonNull(userId, "User ID can't be null");
        Objects.requireNonNull(expiresAt, "Expires at can't be null");
    }
}
