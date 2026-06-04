package io.github.nujanzh.yotsubato.dto.message;

import java.util.List;

public record MessageHistoryResponse(
        List<MessageResponse> messages, String cursor, boolean hasMore) {}
