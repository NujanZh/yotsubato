package io.github.nujanzh.yotsubato.repository.room;

import io.github.nujanzh.yotsubato.model.room.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {}
