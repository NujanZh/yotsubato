package io.github.nujanzh.yotsubato.repository.message;

import io.github.nujanzh.yotsubato.model.message.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query(
            """
        SELECT m FROM Message m
        WHERE m.room.id = :roomId
            AND (m.sentAt < :beforeSentAt
                OR (m.sentAt = :beforeSentAt AND m.id < :beforeId))
        ORDER BY m.sentAt DESC, m.id DESC
    """)
    List<Message> findRoomMessagesBefore(
            UUID roomId, Instant beforeSentAt, UUID beforeId, Pageable pageable);

    @Query(
            """
        SELECT m FROM Message m
        WHERE m.room.id IN :roomIds
            AND NOT EXISTS(
                SELECT 1 FROM Message m2
                WHERE m2.room.id = m.room.id
                    AND (m2.sentAt > m.sentAt
                        OR (m2.sentAt = m.sentAt AND m2.id > m.id))
                )
    """)
    List<Message> findLatestMessagesInRooms(List<UUID> roomIds);

    Optional<Message> findByIdAndRoomId(UUID messageId, UUID roomId);

    Optional<Message> findFirstByRoomIdOrderBySentAtDesc(UUID roomId);
}
