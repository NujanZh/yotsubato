package io.github.nujanzh.yotsubato.dto.message;

import io.github.nujanzh.yotsubato.model.message.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendMessageRequest(
        @NotBlank @Size(max = 4000) String content, @NotNull MessageType type, UUID replyToId) {}
