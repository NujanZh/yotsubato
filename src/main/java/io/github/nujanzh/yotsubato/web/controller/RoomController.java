package io.github.nujanzh.yotsubato.web.controller;

import io.github.nujanzh.yotsubato.dto.room.CreateDmRequest;
import io.github.nujanzh.yotsubato.dto.room.CreateRoomRequest;
import io.github.nujanzh.yotsubato.dto.room.DmResult;
import io.github.nujanzh.yotsubato.dto.room.RoomResponse;
import io.github.nujanzh.yotsubato.security.userdetails.AuthenticatedPrincipal;
import io.github.nujanzh.yotsubato.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @RequestBody @Valid CreateRoomRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        RoomResponse response =
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
    public ResponseEntity<RoomResponse> createDirectMessageRoom(
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
}
