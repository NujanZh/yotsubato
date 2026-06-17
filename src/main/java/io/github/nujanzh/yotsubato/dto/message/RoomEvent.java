package io.github.nujanzh.yotsubato.dto.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = RoomEvent.MessageCreatedEvent.class, name = "MESSAGE_CREATED"),
    @JsonSubTypes.Type(value = RoomEvent.MessageDeletedEvent.class, name = "MESSAGE_DELETED")
})
public sealed interface RoomEvent
        permits RoomEvent.MessageCreatedEvent, RoomEvent.MessageDeletedEvent {

    record MessageCreatedEvent(MessageResponse message) implements RoomEvent {}

    record MessageDeletedEvent(List<UUID> messageIds) implements RoomEvent {}
}
