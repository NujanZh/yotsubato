package io.github.nujanzh.yotsubato.repository.room;

import io.github.nujanzh.yotsubato.model.room.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoomMemberRepository extends JpaRepository<RoomMember, UUID> {}
