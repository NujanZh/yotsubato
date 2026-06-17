package io.github.nujanzh.yotsubato.dto.message;

import io.github.nujanzh.yotsubato.model.message.MessageType;
import java.time.Instant;
import java.util.UUID;

public record MessageDto(
        UUID id,
        UUID clientMessageId,
        UUID roomId,
        UUID senderId,
        String senderUsername,
        String content,
        MessageType type,
        Instant sentAt,
        UUID replyToId) {}
