package io.github.nujanzh.yotsubato.repository;

import io.github.nujanzh.yotsubato.model.message.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {}
