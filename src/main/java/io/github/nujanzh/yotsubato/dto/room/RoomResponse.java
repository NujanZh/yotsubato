package io.github.nujanzh.yotsubato.dto.room;

import io.github.nujanzh.yotsubato.dto.member.MemberInfo;
import io.github.nujanzh.yotsubato.model.room.RoomType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoomResponse(
        UUID id,
        String name,
        RoomType type,
        String description,
        Instant createdAt,
        List<MemberInfo> members) {}
