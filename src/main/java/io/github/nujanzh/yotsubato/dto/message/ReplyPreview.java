package io.github.nujanzh.yotsubato.dto.message;

import java.util.UUID;

public record ReplyPreview(UUID id, String senderUsername, String snippet) {}
