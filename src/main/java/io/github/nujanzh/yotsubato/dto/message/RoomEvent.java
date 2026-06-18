package io.github.nujanzh.yotsubato.dto.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = RoomEvent.MessageCreatedEvent.class, name = "MESSAGE_CREATED"),
    @JsonSubTypes.Type(value = RoomEvent.MessageEditedEvent.class, name = "MESSAGE_EDITED"),
    @JsonSubTypes.Type(value = RoomEvent.MessageDeletedEvent.class, name = "MESSAGE_DELETED"),
    @JsonSubTypes.Type(value = RoomEvent.UserTypingEvent.class, name = "USER_TYPING")
})
public sealed interface RoomEvent
        permits RoomEvent.MessageCreatedEvent,
                RoomEvent.MessageDeletedEvent,
                RoomEvent.MessageEditedEvent,
                RoomEvent.UserTypingEvent {

    record MessageCreatedEvent(MessageResponse message) implements RoomEvent {}

    record MessageEditedEvent(MessageResponse message) implements RoomEvent {}

    record MessageDeletedEvent(List<UUID> messageIds) implements RoomEvent {}

    record UserTypingEvent(UUID userId, boolean typing) implements RoomEvent {}
}
