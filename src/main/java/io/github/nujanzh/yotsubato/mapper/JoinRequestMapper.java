package io.github.nujanzh.yotsubato.mapper;

import io.github.nujanzh.yotsubato.dto.request.JoinRequestResponse;
import io.github.nujanzh.yotsubato.model.request.JoinRequest;

import java.util.List;

public class JoinRequestMapper {
    private JoinRequestMapper() {}

    public static JoinRequestResponse toJoinRequestResponse(JoinRequest joinRequest) {
        return new JoinRequestResponse(
                joinRequest.getId(),
                joinRequest.getUser().getId(),
                joinRequest.getRoom().getId(),
                joinRequest.getUser().getUsername(),
                joinRequest.getStatus(),
                joinRequest.getRequestedAt(),
                joinRequest.getReviewedAt(),
                joinRequest.getRejectionReason());
    }

    public static List<JoinRequestResponse> toJoinRequestResponseList(
            List<JoinRequest> joinRequests) {
        return joinRequests.stream().map(JoinRequestMapper::toJoinRequestResponse).toList();
    }
}
