package io.github.nujanzh.yotsubato.dto.message;

import jakarta.validation.constraints.Size;

public record StompError(int code, String message, @Size(max = 64) String clientMessageId) {}
