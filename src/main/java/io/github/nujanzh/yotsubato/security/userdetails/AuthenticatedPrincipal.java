package io.github.nujanzh.yotsubato.security.userdetails;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

public record AuthenticatedPrincipal(UUID userId, Instant expiresAt) implements Principal {
    @Override
    public String getName() {
        return userId.toString();
    }
}
