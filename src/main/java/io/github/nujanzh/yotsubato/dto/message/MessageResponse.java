package io.github.nujanzh.yotsubato.dto.message;

import io.github.nujanzh.yotsubato.model.message.MessageType;
import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID roomId,
        MessageSender sender,
        String content,
        MessageType type,
        Instant sentAt,
        Instant editedAt,
        ReplyPreview replyTo,
        String clientMessageId) {

    public MessageResponse withClientMessageId(String clientMessageId) {
        return new MessageResponse(
                id, roomId, sender, content, type, sentAt, editedAt, replyTo, clientMessageId);
    }
}
