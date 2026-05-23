package io.github.nujanzh.yotsubato.repository.message;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageStatusRepository
        extends JpaRepository<io.github.nujanzh.yotsubato.model.message.MessageStatus, UUID> {}
