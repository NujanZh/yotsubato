package io.github.nujanzh.yotsubato.repository;

import io.github.nujanzh.yotsubato.model.message.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {}
