package com.esvar.dekanat.mail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MailAttachmentMetaRepository extends JpaRepository<MailAttachmentMetaEntity, Long> {

    List<MailAttachmentMetaEntity> findByMessage_Id(Long messageId);

    MailAttachmentMetaEntity findByMessage_MessageIdAndPartId(String messageId, String partId);
}
