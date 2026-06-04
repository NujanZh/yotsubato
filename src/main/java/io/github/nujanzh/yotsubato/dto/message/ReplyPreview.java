package io.github.nujanzh.yotsubato.dto.message;

import io.github.nujanzh.yotsubato.model.message.Message;

import java.util.UUID;

public record ReplyPreview(UUID id, String senderUsername, String snippet, boolean deleted) {

    private static final int SNIPPET_LENGTH = 100;

    public static ReplyPreview from(Message message) {
        return new ReplyPreview(
                message.getId(),
                message.getSender().getUsername(),
                toSnippet(message.getContent()),
                false);
    }

    public static ReplyPreview deleted(UUID id) {
        return new ReplyPreview(id, null, null, true);
    }

    private static String toSnippet(String text) {
        if (text == null || text.isBlank()) return "";
        return text.length() > SNIPPET_LENGTH ? text.substring(0, SNIPPET_LENGTH) + "..." : text;
    }
}
