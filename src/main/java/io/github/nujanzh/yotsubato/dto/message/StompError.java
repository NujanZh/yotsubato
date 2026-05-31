package io.github.nujanzh.yotsubato.dto.message;

public record StompError(int code, String message, String clientMessageId) {}
