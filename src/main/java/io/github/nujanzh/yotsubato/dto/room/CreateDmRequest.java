package io.github.nujanzh.yotsubato.dto.room;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateDmRequest(@NotNull(message = "User ID must not be null") UUID userId) {}
