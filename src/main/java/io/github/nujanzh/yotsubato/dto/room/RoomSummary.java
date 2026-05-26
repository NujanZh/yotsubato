package io.github.nujanzh.yotsubato.dto.room;

import io.github.nujanzh.yotsubato.model.room.RoomType;

import java.time.Instant;
import java.util.UUID;

public record RoomSummary(
        UUID id,
        String name,
        RoomType type,
        Instant lastMessageAt,
        String lastMessagePreview,
        int memberCount) {}
