package io.github.nujanzh.yotsubato.service;

import io.github.nujanzh.yotsubato.dto.member.MemberInfo;
import io.github.nujanzh.yotsubato.dto.room.*;
import io.github.nujanzh.yotsubato.exception.*;
import io.github.nujanzh.yotsubato.mapper.RoomMapper;
import io.github.nujanzh.yotsubato.model.message.Message;
import io.github.nujanzh.yotsubato.model.room.MemberRole;
import io.github.nujanzh.yotsubato.model.room.Room;
import io.github.nujanzh.yotsubato.model.room.RoomMember;
import io.github.nujanzh.yotsubato.model.room.RoomType;
import io.github.nujanzh.yotsubato.model.user.User;
import io.github.nujanzh.yotsubato.repository.message.MessageRepository;
import io.github.nujanzh.yotsubato.repository.room.RoomMemberRepository;
import io.github.nujanzh.yotsubato.repository.room.RoomRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserService userService;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageRepository messageRepository;
    private final DmTransactions dmTransactions;

    public RoomService(
            RoomRepository roomRepository,
            UserService userService,
            RoomMemberRepository roomMemberRepository,
            MessageRepository messageRepository,
            DmTransactions dmTransactions) {
        this.roomRepository = roomRepository;
        this.userService = userService;
        this.roomMemberRepository = roomMemberRepository;
        this.messageRepository = messageRepository;
        this.dmTransactions = dmTransactions;
    }

    @Transactional
    public RoomDetail createRoom(
            UUID creatorId,
            String name,
            RoomType type,
            String description,
            List<UUID> initialMemberIds) {

        if (type == RoomType.DIRECT) {
            throw new IllegalArgumentException("Direct room must have exactly one other member");
        }

        Set<UUID> uniqueMemberIds = new LinkedHashSet<>(initialMemberIds);
        uniqueMemberIds.remove(creatorId);

        List<User> initialMembers = userService.getAllByIdsOrThrow(uniqueMemberIds);

        User creator = userService.getById(creatorId);

        Room room = new Room();
        room.setName(name);
        room.setType(type);
        room.setDescription(description);
        room.setCreatedBy(creator);
        roomRepository.save(room);

        List<RoomMember> members = new ArrayList<>();
        members.add(RoomMember.of(room, creator, MemberRole.ADMIN));

        initialMembers.stream()
                .map(user -> RoomMember.of(room, user, MemberRole.MEMBER))
                .forEach(members::add);

        List<RoomMember> savedMembers = roomMemberRepository.saveAll(members);
        return RoomMapper.toRoomDetail(room, savedMembers);
    }

    public DmResult getOrCreateDm(UUID callerId, UUID otherUserId) {
        if (callerId.equals(otherUserId)) {
            throw new IllegalArgumentException("Cannot create DM with self");
        }

        try {
            return dmTransactions.tryToCreateDm(callerId, otherUserId);
        } catch (DataIntegrityViolationException ex) {
            return dmTransactions.fetchExistingDm(callerId, otherUserId);
        }
    }

    @Transactional(readOnly = true)
    public List<RoomSummary> getAllRoomsByUserId(UUID userId) {
        List<Room> rooms = roomRepository.findByMembersUserIdOrderByCreatedAtDesc(userId);

        if (rooms.isEmpty()) {
            return List.of();
        }

        List<UUID> roomIds = rooms.stream().map(Room::getId).toList();
        Map<UUID, Message> latestByRoom =
                messageRepository.findLatestMessagesInRooms(roomIds).stream()
                        .collect(
                                Collectors.toMap(
                                        m -> m.getRoom().getId(),
                                        Function.identity(),
                                        (a, b) -> a));

        Map<UUID, Integer> memberCounts =
                roomMemberRepository.countMembersByRoomIds(roomIds).stream()
                        .collect(
                                Collectors.toMap(
                                        RoomMemberCount::getRoomId, c -> c.getCount().intValue()));

        return RoomMapper.toRoomSummaryList(rooms, latestByRoom, memberCounts);
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoom(UUID roomId, UUID callerId) {
        Room room =
                roomRepository
                        .findById(roomId)
                        .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        boolean isMember = roomMemberRepository.existsByRoomIdAndUserId(roomId, callerId);

        if (isMember) {
            return RoomMapper.toRoomDetail(room, roomMemberRepository.findByRoomId(roomId));
        }

        long memberCount = roomMemberRepository.countByRoomId(roomId);

        return switch (room.getType()) {
            case PUBLIC, PRIVATE -> RoomMapper.toPreview(room, memberCount);
            case DIRECT -> throw new RoomNotFoundException("Room not found: " + roomId);
        };
    }

    @Transactional
    public MemberInfo addMember(UUID roomId, UUID callerId, UUID targetUserId) {
        Room room =
                roomRepository
                        .findById(roomId)
                        .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        if (room.getType() == RoomType.DIRECT) {
            throw new RoomOperationException("You can't add more members to a direct room");
        }

        if (!roomMemberRepository.existsByRoomIdAndUserIdAndRole(
                roomId, callerId, MemberRole.ADMIN)) {
            throw new RoomAccessDeniedException("Only admins can add members to a room");
        }

        return doAddMember(room, targetUserId);
    }

    @Transactional
    public MemberInfo joinPublicRoom(UUID roomId, UUID callerId) {
        Room room =
                roomRepository
                        .findById(roomId)
                        .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        if (room.getType() != RoomType.PUBLIC) {
            throw new RoomOperationException("Room is not a public");
        }

        return doAddMember(room, callerId);
    }

    private MemberInfo doAddMember(Room room, UUID userId) {
        if (roomMemberRepository.existsByRoomIdAndUserId(room.getId(), userId)) {
            throw new UserAlreadyMemberException("User is already a member of this room");
        }

        User user = userService.getById(userId);
        RoomMember member = RoomMember.of(room, user, MemberRole.MEMBER);
        roomMemberRepository.save(member);
        return RoomMapper.toMemberInfo(member);
    }

    @Transactional
    public MemberInfo leaveRoom(UUID roomId, UUID callerId) {
        Room room =
                roomRepository
                        .findById(roomId)
                        .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        RoomMember caller =
                roomMemberRepository
                        .findByRoomIdAndUserId(roomId, callerId)
                        .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        if (room.getType() == RoomType.DIRECT) {
            throw new RoomOperationException("You can't leave a direct room");
        }

        long memberCount = roomMemberRepository.countByRoomId(roomId);

        if (memberCount == 1) {
            roomMemberRepository.deleteByRoomIdAndUserId(roomId, callerId);
            roomRepository.deleteById(roomId);
            return RoomMapper.toMemberInfo(caller);
        }

        if (caller.getRole() == MemberRole.ADMIN) {
            long adminCount = roomMemberRepository.countByRoomIdAndRole(roomId, MemberRole.ADMIN);

            if (adminCount == 1) {
                roomMemberRepository
                        .findFirstByRoomIdAndRoleOrderByJoinedAtAsc(roomId, MemberRole.MEMBER)
                        .ifPresent(m -> m.setRole(MemberRole.ADMIN));
            }
        }

        MemberInfo memberInfo = RoomMapper.toMemberInfo(caller);
        roomMemberRepository.deleteByRoomIdAndUserId(roomId, callerId);

        return memberInfo;
    }

    @Transactional
    public MemberInfo removeMember(UUID roomId, UUID callerId, UUID targetUserId) {
        Room room =
                roomRepository
                        .findById(roomId)
                        .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        RoomMember caller =
                roomMemberRepository
                        .findByRoomIdAndUserId(roomId, callerId)
                        .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        if (room.getType() == RoomType.DIRECT) {
            throw new RoomOperationException("You can't remove members from a direct room");
        }

        if (caller.getRole() != MemberRole.ADMIN) {
            throw new RoomAccessDeniedException("Only admins can remove members from a room");
        }

        if (callerId.equals(targetUserId)) {
            throw new RoomOperationException("You cannot remove yourself from the room");
        }

        RoomMember target =
                roomMemberRepository
                        .findByRoomIdAndUserId(roomId, targetUserId)
                        .orElseThrow(
                                () ->
                                        new MembershipNotFoundException(
                                                "User is not a member of this room"));

        MemberInfo memberInfo = RoomMapper.toMemberInfo(target);
        roomMemberRepository.deleteByRoomIdAndUserId(roomId, targetUserId);

        return memberInfo;
    }

    @Transactional
    public MemberInfo changeRole(
            UUID roomId, UUID callerId, UUID targetUserId, MemberRole newRole) {

        roomRepository
                .findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        RoomMember caller =
                roomMemberRepository
                        .findByRoomIdAndUserId(roomId, callerId)
                        .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        if (caller.getRole() != MemberRole.ADMIN) {
            throw new RoomAccessDeniedException("Only admins can change roles in a room");
        }

        if (callerId.equals(targetUserId)) {
            throw new RoomOperationException("You cannot change your own role");
        }

        RoomMember targetUser =
                roomMemberRepository
                        .findByRoomIdAndUserId(roomId, targetUserId)
                        .orElseThrow(
                                () ->
                                        new MembershipNotFoundException(
                                                "User is not a member of this room"));

        if (targetUser.getRole() == newRole) {
            throw new RoomOperationException("User already has the requested role");
        }

        targetUser.setRole(newRole);
        return RoomMapper.toMemberInfo(targetUser);
    }
}
