package io.github.nujanzh.yotsubato.mapper;

import io.github.nujanzh.yotsubato.dto.member.MemberInfo;
import io.github.nujanzh.yotsubato.dto.room.RoomDetail;
import io.github.nujanzh.yotsubato.dto.room.RoomSummary;
import io.github.nujanzh.yotsubato.model.message.Message;
import io.github.nujanzh.yotsubato.model.room.Room;
import io.github.nujanzh.yotsubato.model.room.RoomMember;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RoomMapper {

    private RoomMapper() {}

    public static RoomDetail mapToRoomResponse(Room room, List<RoomMember> members) {
        List<MemberInfo> memberInfos = members.stream().map(RoomMapper::mapToMemberInfo).toList();
        return new RoomDetail(
                room.getId(),
                room.getName(),
                room.getType(),
                room.getDescription(),
                room.getCreatedAt(),
                memberInfos);
    }

    public static MemberInfo mapToMemberInfo(RoomMember member) {
        return new MemberInfo(
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getRole(),
                member.getJoinedAt());
    }

    public static RoomSummary mapToRoomSummary(Room room, Message latestMessage) {
        return new RoomSummary(
                room.getId(),
                room.getName(),
                room.getType(),
                latestMessage != null ? latestMessage.getSentAt() : null,
                latestMessage != null ? latestMessage.getContent() : null);
    }

    public static List<RoomSummary> mapToRoomSummaryList(
            List<Room> rooms, Map<UUID, Message> latestByRoomId) {
        return rooms.stream()
                .map(
                        room -> {
                            Message last = latestByRoomId.get(room.getId());

                            return new RoomSummary(
                                    room.getId(),
                                    room.getName(),
                                    room.getType(),
                                    last != null ? last.getSentAt() : null,
                                    last != null ? last.getContent() : null);
                        })
                .toList();
    }
}
