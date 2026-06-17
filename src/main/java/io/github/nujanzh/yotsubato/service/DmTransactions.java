package io.github.nujanzh.yotsubato.service;

import io.github.nujanzh.yotsubato.dto.room.DmResult;
import io.github.nujanzh.yotsubato.exception.RoomNotFoundException;
import io.github.nujanzh.yotsubato.mapper.RoomMapper;
import io.github.nujanzh.yotsubato.model.room.MemberRole;
import io.github.nujanzh.yotsubato.model.room.Room;
import io.github.nujanzh.yotsubato.model.room.RoomMember;
import io.github.nujanzh.yotsubato.model.room.RoomType;
import io.github.nujanzh.yotsubato.model.user.User;
import io.github.nujanzh.yotsubato.repository.room.RoomMemberRepository;
import io.github.nujanzh.yotsubato.repository.room.RoomRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DmTransactions {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserService userService;

    public DmTransactions(
            RoomRepository roomRepository,
            RoomMemberRepository roomMemberRepository,
            UserService userService) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.userService = userService;
    }

    @Transactional
    public DmResult tryToCreateDm(UUID callerId, UUID otherUserId) {
        String key = Room.createDmKey(callerId, otherUserId);

        Optional<Room> existing = roomRepository.findByDmKey(key);

        if (existing.isPresent()) {
            List<RoomMember> members = roomMemberRepository.findByRoomId(existing.get().getId());
            return new DmResult(RoomMapper.toRoomDetail(existing.get(), members), false);
        }

        User caller = userService.getById(callerId);
        User otherUser = userService.getById(otherUserId);

        Room newRoom = new Room();
        newRoom.setType(RoomType.DIRECT);
        newRoom.setCreatedBy(caller);
        newRoom.setDmKey(key);
        roomRepository.saveAndFlush(newRoom);

        List<RoomMember> saved =
                roomMemberRepository.saveAll(
                        List.of(
                                RoomMember.of(newRoom, caller, MemberRole.MEMBER),
                                RoomMember.of(newRoom, otherUser, MemberRole.MEMBER)));

        return new DmResult(RoomMapper.toRoomDetail(newRoom, saved), true);
    }

    @Transactional
    public DmResult fetchExistingDm(UUID callerId, UUID otherUserId) {
        Room room =
                roomRepository
                        .findByDmKey(Room.createDmKey(callerId, otherUserId))
                        .orElseThrow(() -> new RoomNotFoundException("Room not found"));

        List<RoomMember> members = roomMemberRepository.findByRoomId(room.getId());
        return new DmResult(RoomMapper.toRoomDetail(room, members), false);
    }
}
