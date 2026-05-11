package io.github.nujanzh.yotsubato.repository;

import io.github.nujanzh.yotsubato.model.message.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MesseageStatus extends JpaRepository<MessageStatus, UUID> {}
