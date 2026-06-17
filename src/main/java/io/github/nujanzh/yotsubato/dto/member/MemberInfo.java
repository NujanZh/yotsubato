package io.github.nujanzh.yotsubato.dto.member;

import io.github.nujanzh.yotsubato.model.room.MemberRole;
import java.time.Instant;
import java.util.UUID;

public record MemberInfo(UUID userId, String username, MemberRole role, Instant joinedAt) {}
