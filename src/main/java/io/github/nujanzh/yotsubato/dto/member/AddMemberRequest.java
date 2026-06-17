package io.github.nujanzh.yotsubato.dto.member;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddMemberRequest(@NotNull UUID userId) {}
