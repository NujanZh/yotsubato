package io.github.nujanzh.yotsubato.dto.room;

import java.util.UUID;

public interface RoomMemberCount {
    UUID getRoomId();

    Long getCount();
}
