package io.github.nujanzh.yotsubato.service;

import io.github.nujanzh.yotsubato.dto.room.*;
import io.github.nujanzh.yotsubato.exception.RoomNotFoundException;
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
import io.github.nujanzh.yotsubato.web.service.UserService;
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

    public RoomService(
            RoomRepository roomRepository,
            UserService userService,
            RoomMemberRepository roomMemberRepository,
            MessageRepository messageRepository) {
        this.roomRepository = roomRepository;
        this.userService = userService;
        this.roomMemberRepository = roomMemberRepository;
        this.messageRepository = messageRepository;
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

    @Transactional
    public DmResult getOrCreateDm(UUID callerId, UUID otherUserId) {
        if (callerId.equals(otherUserId)) {
            throw new IllegalArgumentException("Cannot create DM with self");
        }

        Optional<Room> room = roomRepository.findDmBetween(callerId, otherUserId, RoomType.DIRECT);

        if (room.isPresent()) {
            List<RoomMember> members = roomMemberRepository.findByRoomId(room.get().getId());
            RoomDetail roomDetail = RoomMapper.toRoomDetail(room.get(), members);
            return new DmResult(roomDetail, false);
        }

        User caller = userService.getById(callerId);
        User otherUser = userService.getById(otherUserId);

        Room newRoom = new Room();
        newRoom.setType(RoomType.DIRECT);
        newRoom.setCreatedBy(caller);
        roomRepository.save(newRoom);

        List<RoomMember> saved =
                roomMemberRepository.saveAll(
                        List.of(
                                RoomMember.of(newRoom, caller, MemberRole.MEMBER),
                                RoomMember.of(newRoom, otherUser, MemberRole.MEMBER)));

        return new DmResult(RoomMapper.toRoomDetail(newRoom, saved), true);
    }

    public List<RoomSummary> getAllRoomsByUserId(UUID userId) {
        List<Room> rooms = roomRepository.findByMembersUserIdOrderByCreatedAtDesc(userId);

        if (rooms.isEmpty()) {
            return List.of();
        }

        List<UUID> roomIds = rooms.stream().map(Room::getId).toList();
        Map<UUID, Message> latestByRoom =
                messageRepository.findLatestMessagesInRooms(roomIds).stream()
                        .collect(Collectors.toMap(m -> m.getRoom().getId(), Function.identity()));

        Map<UUID, Integer> memberCounts =
                roomMemberRepository.countMembersByRoomIds(roomIds).stream()
                        .collect(
                                Collectors.toMap(
                                        RoomMemberCount::getRoomId, c -> c.getCount().intValue()));

        return RoomMapper.toRoomSummaryList(rooms, latestByRoom, memberCounts);
    }

    public RoomResponse getRoom(UUID roomId, UUID callerId) {
        Room room =
                roomRepository
                        .findById(roomId)
                        .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        boolean isMember = roomMemberRepository.existsByRoomIdAndUserId(roomId, callerId);

        if (isMember) {
            return RoomMapper.toRoomDetail(room, roomMemberRepository.findByRoomId(roomId));
        }

        int memberCount = roomMemberRepository.countByRoomId(roomId);

        return switch (room.getType()) {
            case PUBLIC, PRIVATE -> RoomMapper.toPreview(room, memberCount);
            case DIRECT -> throw new RoomNotFoundException("Room not found: " + roomId);
        };
    }
}
