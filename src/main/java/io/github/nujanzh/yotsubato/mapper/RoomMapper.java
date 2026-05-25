package io.github.nujanzh.yotsubato.mapper;

import io.github.nujanzh.yotsubato.dto.member.MemberInfo;
import io.github.nujanzh.yotsubato.dto.room.RoomResponse;
import io.github.nujanzh.yotsubato.model.room.Room;
import io.github.nujanzh.yotsubato.model.room.RoomMember;

import java.util.List;

public class RoomMapper {

    private RoomMapper() {}

    public static RoomResponse mapToRoomResponse(Room room, List<RoomMember> members) {
        List<MemberInfo> memberInfos = members.stream().map(RoomMapper::mapToMemberInfo).toList();
        return new RoomResponse(
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
}
