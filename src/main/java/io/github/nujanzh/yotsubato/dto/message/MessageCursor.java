package io.github.nujanzh.yotsubato.dto.message;

import io.github.nujanzh.yotsubato.exception.InvalidCursorException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public record MessageCursor(Instant sentAt, UUID lastId) {

    public String encode() {
        String raw = sentAt.toEpochMilli() + "_" + lastId;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static MessageCursor decode(String token) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            int sep = raw.indexOf('_');
            if (sep < 0) {
                throw new IllegalArgumentException("missing separator");
            }
            Instant sentAt = Instant.ofEpochMilli(Long.parseLong(raw.substring(0, sep)));
            UUID lastId = UUID.fromString(raw.substring(sep + 1));
            return new MessageCursor(sentAt, lastId);
        } catch (IllegalArgumentException e) {
            throw new InvalidCursorException(token);
        }
    }
}
