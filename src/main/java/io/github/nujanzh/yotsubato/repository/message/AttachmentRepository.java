package io.github.nujanzh.yotsubato.repository.message;

import io.github.nujanzh.yotsubato.model.message.Attachment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {}
