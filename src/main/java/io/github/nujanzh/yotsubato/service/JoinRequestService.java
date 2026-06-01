package io.github.nujanzh.yotsubato.service;

import io.github.nujanzh.yotsubato.dto.member.MemberInfo;
import io.github.nujanzh.yotsubato.dto.joinrequest.JoinRequestResponse;
import io.github.nujanzh.yotsubato.exception.*;
import io.github.nujanzh.yotsubato.mapper.JoinRequestMapper;
import io.github.nujanzh.yotsubato.mapper.RoomMapper;
import io.github.nujanzh.yotsubato.model.joinrequest.JoinRequest;
import io.github.nujanzh.yotsubato.model.joinrequest.JoinRequestStatus;
import io.github.nujanzh.yotsubato.model.room.MemberRole;
import io.github.nujanzh.yotsubato.model.room.Room;
import io.github.nujanzh.yotsubato.model.room.RoomMember;
import io.github.nujanzh.yotsubato.model.room.RoomType;
import io.github.nujanzh.yotsubato.model.user.User;
import io.github.nujanzh.yotsubato.repository.joinrequest.JoinRequestRepository;
import io.github.nujanzh.yotsubato.repository.room.RoomMemberRepository;
import io.github.nujanzh.yotsubato.repository.room.RoomRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JoinRequestService {

    private final JoinRequestRepository joinRequestRepository;
    private final UserService userService;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;

    public JoinRequestService(
            JoinRequestRepository joinRequestRepository,
            UserService userService,
            RoomRepository roomRepository,
            RoomMemberRepository roomMemberRepository) {
        this.joinRequestRepository = joinRequestRepository;
        this.userService = userService;
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
    }

    @Transactional
    public JoinRequestResponse requestJoin(UUID roomId, UUID callerId) {
        Room room =
                roomRepository
                        .findById(roomId)
                        .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        if (room.getType() != RoomType.PRIVATE) {
            throw new RoomOperationException(
                    "Room is not a private, user can join without a request");
        }

        boolean isMember = roomMemberRepository.existsByRoomIdAndUserId(roomId, callerId);
        if (isMember) {
            throw new UserAlreadyMemberException("User is already a member of this room");
        }

        boolean isRequestExist =
                joinRequestRepository.existsByRoomIdAndUserIdAndStatus(
                        roomId, callerId, JoinRequestStatus.PENDING);

        if (isRequestExist) {
            throw new DuplicateJoinRequestException("User already has a pending request");
        }

        User user = userService.getById(callerId);

        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setUser(user);
        joinRequest.setRoom(room);
        joinRequest.setStatus(JoinRequestStatus.PENDING);

        JoinRequest savedJoinRequest;

        try {
            savedJoinRequest = joinRequestRepository.save(joinRequest);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateJoinRequestException("User already has a pending request");
        }

        return JoinRequestMapper.toJoinRequestResponse(savedJoinRequest);
    }

    @Transactional(readOnly = true)
    public List<JoinRequestResponse> listPending(UUID roomId, UUID callerId) {

        if (!roomRepository.existsById(roomId)) {
            throw new RoomNotFoundException("Room not found: " + roomId);
        }

        requireAdmin(roomId, callerId);

        List<JoinRequest> joinRequestList =
                joinRequestRepository.findByRoomIdAndStatus(roomId, JoinRequestStatus.PENDING);
        return JoinRequestMapper.toJoinRequestResponseList(joinRequestList);
    }

    @Transactional
    public MemberInfo approve(UUID roomId, UUID requestId, UUID callerId) {
        JoinRequest request = requireJoinRequest(roomId, requestId);
        RoomMember caller = requireAdmin(roomId, callerId);

        if (request.getStatus() != JoinRequestStatus.PENDING) {
            throw new RoomOperationException("Request is not pending");
        }

        if (roomMemberRepository.existsByRoomIdAndUserId(roomId, request.getUser().getId())) {
            throw new UserAlreadyMemberException("User is already a member of this room");
        }

        RoomMember member = new RoomMember();
        member.setRoom(request.getRoom());
        member.setUser(request.getUser());
        member.setRole(MemberRole.MEMBER);
        RoomMember savedMember;

        try {
            savedMember = roomMemberRepository.save(member);
        } catch (DataIntegrityViolationException ex) {
            throw new UserAlreadyMemberException("User already has a pending request");
        }

        request.setStatus(JoinRequestStatus.APPROVED);
        request.setReviewedAt(Instant.now());
        request.setReviewedBy(caller.getUser());

        return RoomMapper.toMemberInfo(savedMember);
    }

    @Transactional
    public JoinRequestResponse reject(UUID roomId, UUID requestId, UUID callerId, String reason) {
        JoinRequest request = requireJoinRequest(roomId, requestId);

        if (request.getStatus() != JoinRequestStatus.PENDING) {
            throw new RoomOperationException("Request is not pending");
        }

        RoomMember caller = requireAdmin(roomId, callerId);

        request.setStatus(JoinRequestStatus.REJECTED);
        request.setReviewedAt(Instant.now());
        request.setReviewedBy(caller.getUser());
        request.setRejectionReason(reason);

        return JoinRequestMapper.toJoinRequestResponse(request);
    }

    @Transactional
    public JoinRequestResponse cancel(UUID roomId, UUID requestId, UUID callerId) {
        JoinRequest request = requireJoinRequest(roomId, requestId);

        if (!request.getUser().getId().equals(callerId)) {
            throw new RoomAccessDeniedException("Only the user who made the request can cancel it");
        }

        if (request.getStatus() == JoinRequestStatus.APPROVED) {
            throw new RoomOperationException("Request has already been approved");
        }

        if (request.getStatus() == JoinRequestStatus.REJECTED) {
            throw new RoomOperationException("Request has already been rejected");
        }

        // TODO: Better to just change status to CANCELED?
        // Because if we deleting canceled requests, so we need also delete rejected requests?
        JoinRequestResponse response = JoinRequestMapper.toJoinRequestResponse(request);
        joinRequestRepository.delete(request);

        return response;
    }

    private RoomMember requireAdmin(UUID roomId, UUID callerId) {
        RoomMember caller =
                roomMemberRepository
                        .findByRoomIdAndUserId(roomId, callerId)
                        .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));
        if (caller.getRole() != MemberRole.ADMIN) {
            throw new RoomAccessDeniedException("Only admins can perform this action");
        }
        return caller;
    }

    private JoinRequest requireJoinRequest(UUID roomId, UUID requestId) {
        JoinRequest request =
                joinRequestRepository
                        .findById(requestId)
                        .orElseThrow(
                                () ->
                                        new JoinRequestNotFoundException(
                                                "Request not found: " + requestId));

        if (!request.getRoom().getId().equals(roomId)) {
            throw new JoinRequestNotFoundException("Request not found: " + requestId);
        }

        return request;
    }
}
