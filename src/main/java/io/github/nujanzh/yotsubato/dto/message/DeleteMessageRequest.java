package io.github.nujanzh.yotsubato.dto.message;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.hibernate.validator.constraints.UniqueElements;

public record DeleteMessageRequest(
        @NotEmpty @Size(max = 100) @UniqueElements List<UUID> messageIds) {}
