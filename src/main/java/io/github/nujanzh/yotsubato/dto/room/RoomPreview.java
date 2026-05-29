package io.github.nujanzh.yotsubato.dto.room;

import io.github.nujanzh.yotsubato.model.room.RoomType;

import java.util.UUID;

public record RoomPreview(UUID id, String name, RoomType type, String description, long memberCount)
        implements RoomResponse {}
