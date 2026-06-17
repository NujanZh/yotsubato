package io.github.nujanzh.yotsubato.repository.room;

import io.github.nujanzh.yotsubato.dto.room.RoomMemberCount;
import io.github.nujanzh.yotsubato.model.room.MemberRole;
import io.github.nujanzh.yotsubato.model.room.RoomMember;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoomMemberRepository extends JpaRepository<RoomMember, UUID> {

    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);

    boolean existsByRoomIdAndUserIdAndRole(UUID roomId, UUID userId, MemberRole role);

    @EntityGraph(attributePaths = {"user"})
    Optional<RoomMember> findByRoomIdAndUserId(UUID roomId, UUID userId);

    @EntityGraph(attributePaths = {"user"})
    List<RoomMember> findByRoomId(UUID roomId);

    Optional<RoomMember> findFirstByRoomIdAndRoleOrderByJoinedAtAsc(UUID roomId, MemberRole role);

    void deleteByRoomIdAndUserId(UUID roomId, UUID userId);

    int countByRoomId(UUID roomId);

    int countByRoomIdAndRole(UUID roomId, MemberRole role);

    @Query(
            "SELECT rm.room.id AS roomId, COUNT(rm) AS count FROM RoomMember rm WHERE rm.room.id IN"
                    + " :roomIds GROUP BY rm.room.id")
    List<RoomMemberCount> countMembersByRoomIds(Collection<UUID> roomIds);
}
