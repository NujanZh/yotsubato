package io.github.nujanzh.yotsubato.repository.room;

import io.github.nujanzh.yotsubato.model.room.Room;
import io.github.nujanzh.yotsubato.model.room.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findByMembersUserIdOrderByCreatedAtDesc(UUID userId);

    @Query(
            """
        SELECT r FROM Room r
        WHERE r.type = :type
            AND EXISTS (SELECT 1 FROM RoomMember m1 WHERE m1.room = r AND m1.user.id = :userA)
            AND EXISTS (SELECT 1 FROM RoomMember m2 WHERE m2.room = r AND m2.user.id = :userB)
            AND (SELECT COUNT(m3) FROM RoomMember m3 WHERE m3.room = r) = 2
    """)
    Optional<Room> findDmBetween(UUID userA, UUID userB, RoomType type);

    Optional<Room> findByDmKey(String key);
}
