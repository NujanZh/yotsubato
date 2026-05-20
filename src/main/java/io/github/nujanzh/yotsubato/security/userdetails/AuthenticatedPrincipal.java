package io.github.nujanzh.yotsubato.security.userdetails;

import java.time.Instant;
import java.util.UUID;

public record AuthenticatedPrincipal(UUID userId, Instant expiresAt) {}
