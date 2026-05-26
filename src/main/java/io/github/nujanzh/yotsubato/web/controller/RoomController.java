package io.github.nujanzh.yotsubato.web.controller;

import io.github.nujanzh.yotsubato.dto.room.*;
import io.github.nujanzh.yotsubato.security.userdetails.AuthenticatedPrincipal;
import io.github.nujanzh.yotsubato.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomDetail> createRoom(
            @RequestBody @Valid CreateRoomRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        RoomDetail response =
                roomService.createRoom(
                        principal.userId(),
                        request.name(),
                        request.type(),
                        request.description(),
                        request.initialMemberIds());

        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/rooms/" + response.id()))
                .body(response);
    }

    @PostMapping("/dm")
    public ResponseEntity<RoomDetail> createDirectMessageRoom(
            @RequestBody @Valid CreateDmRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        DmResult result = roomService.getOrCreateDm(principal.userId(), request.userId());

        if (result.wasCreated()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .location(URI.create("/api/rooms/dm/" + result.room().id()))
                    .body(result.room());
        }

        return ResponseEntity.status(HttpStatus.OK).body(result.room());
    }

    @GetMapping
    public ResponseEntity<List<RoomSummary>> getRooms(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        List<RoomSummary> rooms = roomService.getAllRoomsByUserId(principal.userId());
        return ResponseEntity.status(HttpStatus.OK).body(rooms);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoom(
            @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        RoomResponse room = roomService.getRoom(id, principal.userId());
        return ResponseEntity.status(HttpStatus.OK).body(room);
    }
}
