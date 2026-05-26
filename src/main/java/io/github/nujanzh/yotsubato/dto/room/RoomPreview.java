package io.github.nujanzh.yotsubato.dto.room;

import java.util.UUID;

public record RoomPreview(UUID id, String name, String type, String description, int memberCount) {}
