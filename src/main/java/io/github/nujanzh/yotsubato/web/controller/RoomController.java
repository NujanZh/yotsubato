package io.github.nujanzh.yotsubato.web.controller;

import io.github.nujanzh.yotsubato.dto.member.AddMemberRequest;
import io.github.nujanzh.yotsubato.dto.member.ChangeRoleRequest;
import io.github.nujanzh.yotsubato.dto.member.MemberInfo;
import io.github.nujanzh.yotsubato.dto.message.*;
import io.github.nujanzh.yotsubato.dto.room.*;
import io.github.nujanzh.yotsubato.security.userdetails.AuthenticatedPrincipal;
import io.github.nujanzh.yotsubato.service.MessageService;
import io.github.nujanzh.yotsubato.service.RoomService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;
    private final MessageService messageService;
    private final SimpMessagingTemplate template;

    public RoomController(
            RoomService roomService,
            MessageService messageService,
            SimpMessagingTemplate template) {
        this.roomService = roomService;
        this.messageService = messageService;
        this.template = template;
    }

    @GetMapping
    public ResponseEntity<List<RoomSummary>> getAllRooms(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        List<RoomSummary> rooms = roomService.getAllRoomsByUserId(principal.userId());
        return ResponseEntity.status(HttpStatus.OK).body(rooms);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoom(
            @PathVariable UUID roomId, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        RoomResponse room = roomService.getRoom(roomId, principal.userId());
        return ResponseEntity.status(HttpStatus.OK).body(room);
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<MessageHistoryResponse> getHistory(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit) {
        MessageCursor decoded =
                (cursor == null || cursor.isBlank()) ? null : MessageCursor.decode(cursor);

        MessageHistoryResponse history =
                messageService.getRoomHistory(roomId, principal.userId(), decoded, limit);

        return ResponseEntity.status(HttpStatus.OK).body(history);
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

    @PostMapping("/{roomId}/members")
    public ResponseEntity<MemberInfo> addMember(
            @PathVariable UUID roomId,
            @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        MemberInfo newMember = roomService.addMember(roomId, principal.userId(), request.userId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/rooms/" + roomId + "/members/" + newMember.userId()))
                .body(newMember);
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<MemberInfo> selfJoin(
            @PathVariable UUID roomId, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        MemberInfo newMember = roomService.joinPublicRoom(roomId, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/rooms/" + roomId + "/members/" + newMember.userId()))
                .body(newMember);
    }

    @PatchMapping("/{roomId}/messages/{messageId}")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable UUID roomId,
            @PathVariable UUID messageId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestBody @Valid UpdateMessageRequest request) {

        MessageResponse response =
                messageService.editMessage(roomId, messageId, principal.userId(), request);

        template.convertAndSend(
                "/topic/rooms/" + roomId, new RoomEvent.MessageEditedEvent(response));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{roomId}/messages/delete")
    public ResponseEntity<Void> deleteMessages(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestBody @Valid DeleteMessageRequest request) {
        List<UUID> deletedMessageIds =
                messageService.deleteMessages(roomId, principal.userId(), request.messageIds());

        template.convertAndSend(
                "/topic/rooms/" + roomId, new RoomEvent.MessageDeletedEvent(deletedMessageIds));

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{roomId}/members/me")
    public ResponseEntity<MemberInfo> leaveRoom(
            @PathVariable UUID roomId, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        MemberInfo memberInfo = roomService.leaveRoom(roomId, principal.userId());
        return ResponseEntity.status(HttpStatus.OK).body(memberInfo);
    }

    @DeleteMapping("/{roomId}/members/{userId}")
    public ResponseEntity<MemberInfo> removeMember(
            @PathVariable UUID roomId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        MemberInfo memberInfo = roomService.removeMember(roomId, principal.userId(), userId);
        return ResponseEntity.status(HttpStatus.OK).body(memberInfo);
    }

    @PatchMapping("/{roomId}/members/{userId}")
    public ResponseEntity<MemberInfo> changeMemberRole(
            @PathVariable UUID roomId,
            @PathVariable UUID userId,
            @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        MemberInfo memberInfo =
                roomService.changeRole(roomId, principal.userId(), userId, request.role());
        return ResponseEntity.status(HttpStatus.OK).body(memberInfo);
    }
}
