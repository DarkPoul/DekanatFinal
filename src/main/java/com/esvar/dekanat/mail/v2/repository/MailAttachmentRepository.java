package com.esvar.dekanat.mail.v2.repository;

import com.esvar.dekanat.mail.v2.entity.MailAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MailAttachmentRepository extends JpaRepository<MailAttachmentEntity, Long> {

    List<MailAttachmentEntity> findByMessageId(Long messageId);

    Optional<MailAttachmentEntity> findByIdAndInlineTrue(Long id);
}
