package io.github.nujanzh.yotsubato.dto.member;

import io.github.nujanzh.yotsubato.model.room.MemberRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull MemberRole role) {}
