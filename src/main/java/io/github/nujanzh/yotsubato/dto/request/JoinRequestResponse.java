package io.github.nujanzh.yotsubato.dto.request;

import io.github.nujanzh.yotsubato.model.request.JoinRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record JoinRequestResponse(
        UUID id,
        UUID userId,
        UUID roomId,
        String username,
        JoinRequestStatus status,
        Instant requestedAt,
        Instant reviewedAt,
        String rejectionReason) {}
