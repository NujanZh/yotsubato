package io.github.nujanzh.yotsubato.repository.room;

import io.github.nujanzh.yotsubato.model.room.RoomMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomMemberRepository extends JpaRepository<RoomMember, UUID> {

    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);

    Optional<RoomMember> findByRoomIdAndUserId(UUID roomId, UUID userId);

    @EntityGraph(attributePaths = {"user"})
    List<RoomMember> findByRoomId(UUID roomId);

    void deleteByRoomIdAndUserId(UUID roomId, UUID userId);
}
