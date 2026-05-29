package io.github.nujanzh.yotsubato.web.controller;

import io.github.nujanzh.yotsubato.dto.member.MemberInfo;
import io.github.nujanzh.yotsubato.dto.request.JoinRequestResponse;
import io.github.nujanzh.yotsubato.dto.request.RejectJoinRequest;
import io.github.nujanzh.yotsubato.security.userdetails.AuthenticatedPrincipal;
import io.github.nujanzh.yotsubato.service.JoinRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/rooms")
public class JoinRequestController {

    private final JoinRequestService joinRequestService;

    public JoinRequestController(JoinRequestService joinRequestService) {
        this.joinRequestService = joinRequestService;
    }

    @GetMapping("/{id}/join-requests")
    public ResponseEntity<List<JoinRequestResponse>> getJoinRequests(
            @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        List<JoinRequestResponse> requests = joinRequestService.listPending(id, principal.userId());
        return ResponseEntity.status(HttpStatus.OK).body(requests);
    }

    @PostMapping("/{id}/join-requests")
    public ResponseEntity<JoinRequestResponse> requestJoin(
            @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        JoinRequestResponse response = joinRequestService.requestJoin(id, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/rooms/" + id + "/join-requests" + response.id()))
                .body(response);
    }

    @PostMapping("/{id}/join-requests/{reqId}/approve")
    public ResponseEntity<MemberInfo> approveJoinRequest(
            @PathVariable UUID id,
            @PathVariable UUID reqId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        MemberInfo newMember = joinRequestService.approve(id, reqId, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/rooms/" + id + "/members" + newMember.userId()))
                .body(newMember);
    }

    @PostMapping("/{id}/join-requests/{reqId}/reject")
    public ResponseEntity<JoinRequestResponse> rejectJoinRequest(
            @PathVariable UUID id,
            @PathVariable UUID reqId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestBody RejectJoinRequest request) {
        JoinRequestResponse response =
                joinRequestService.reject(id, reqId, principal.userId(), request.reason());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}/join-requests/{reqId}")
    public ResponseEntity<JoinRequestResponse> cancelJoinRequest(
            @PathVariable UUID id,
            @PathVariable UUID reqId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        JoinRequestResponse response = joinRequestService.cancel(id, reqId, principal.userId());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
