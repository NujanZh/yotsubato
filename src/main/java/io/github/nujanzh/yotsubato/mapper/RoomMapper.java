package io.github.nujanzh.yotsubato.mapper;

import io.github.nujanzh.yotsubato.dto.member.MemberInfo;
import io.github.nujanzh.yotsubato.dto.room.RoomDetail;
import io.github.nujanzh.yotsubato.dto.room.RoomPreview;
import io.github.nujanzh.yotsubato.dto.room.RoomSummary;
import io.github.nujanzh.yotsubato.model.message.Message;
import io.github.nujanzh.yotsubato.model.room.Room;
import io.github.nujanzh.yotsubato.model.room.RoomMember;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RoomMapper {

    private RoomMapper() {}

    public static RoomDetail toRoomDetail(Room room, List<RoomMember> members) {
        List<MemberInfo> memberInfos = members.stream().map(RoomMapper::toMemberInfo).toList();
        return new RoomDetail(
                room.getId(),
                room.getName(),
                room.getType(),
                room.getDescription(),
                room.getCreatedAt(),
                memberInfos);
    }

    public static MemberInfo toMemberInfo(RoomMember member) {
        return new MemberInfo(
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getRole(),
                member.getJoinedAt());
    }

    public static RoomPreview toPreview(Room room, long memberCount) {
        return new RoomPreview(
                room.getId(), room.getName(), room.getType(), room.getDescription(), memberCount);
    }

    public static RoomSummary toRoomSummary(Room room, Message latestMessage, int memberCount) {
        return new RoomSummary(
                room.getId(),
                room.getName(),
                room.getType(),
                latestMessage != null ? latestMessage.getSentAt() : null,
                latestMessage != null ? latestMessage.getContent() : null,
                memberCount);
    }

    public static List<RoomSummary> toRoomSummaryList(
            List<Room> rooms, Map<UUID, Message> latestByRoomId, Map<UUID, Integer> memberCounts) {
        return rooms.stream()
                .map(
                        room ->
                                toRoomSummary(
                                        room,
                                        latestByRoomId.get(room.getId()),
                                        memberCounts.getOrDefault(room.getId(), 0)))
                .toList();
    }
}
