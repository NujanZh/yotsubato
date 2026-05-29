package io.github.nujanzh.yotsubato.repository.request;

import io.github.nujanzh.yotsubato.model.request.JoinRequest;
import io.github.nujanzh.yotsubato.model.request.JoinRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, UUID> {

    List<JoinRequest> findByRoomIdAndStatus(UUID roomId, JoinRequestStatus status);

    JoinRequest findByRoomIdAndUserIdAndStatus(UUID roomId, UUID userId, JoinRequestStatus status);

    boolean existsByRoomIdAndUserIdAndStatus(UUID roomId, UUID userId, JoinRequestStatus status);

    @EntityGraph(attributePaths = {"user"})
    List<JoinRequest> findByRoomId(UUID roomId);
}
