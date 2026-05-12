package io.github.nujanzh.yotsubato.security;

import java.time.Instant;
import java.util.UUID;

public record AuthenticatedPrincipal(UUID userId, Instant expiresAt) {}
