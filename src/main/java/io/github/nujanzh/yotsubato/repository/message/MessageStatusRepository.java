package io.github.nujanzh.yotsubato.repository.message;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageStatusRepository
        extends JpaRepository<io.github.nujanzh.yotsubato.model.message.MessageStatus, UUID> {}
