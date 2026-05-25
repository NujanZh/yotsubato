package io.github.nujanzh.yotsubato.dto.room;

import io.github.nujanzh.yotsubato.model.room.RoomType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateRoomRequest(
        @Size(min = 1, max = 100, message = "Room name must be between 3 and 50 characters")
                String name,
        @NotNull(message = "Room type is required") RoomType type,
        @Size(max = 500, message = "Description must not exceed 500 characters") String description,
        List<UUID> initialMemberIds) {

    public CreateRoomRequest {
        initialMemberIds = initialMemberIds == null ? List.of() : initialMemberIds;
    }
}
