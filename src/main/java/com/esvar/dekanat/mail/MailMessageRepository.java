package com.esvar.dekanat.mail;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MailMessageRepository extends JpaRepository<MailMessageEntity, Long> {

    Optional<MailMessageEntity> findByMessageId(String messageId);

    Page<MailMessageEntity> findByChatId(Long chatId, Pageable pageable);

    Page<MailMessageEntity> findByChatIdAndSentAtBefore(Long chatId, Instant before, Pageable pageable);

    Optional<MailMessageEntity> findTop1ByChatIdOrderBySentAtDesc(Long chatId);

    Page<MailMessageEntity> findByThreadKey(String threadKey, Pageable pageable);

    Page<MailMessageEntity> findByThreadKeyAndSentAtBefore(String threadKey, Instant before, Pageable pageable);

    Optional<MailMessageEntity> findTop1ByThreadKeyOrderBySentAtDesc(String threadKey);

    List<MailMessageEntity> findByChatIdOrderBySentAtDesc(Long chatId);

    List<MailMessageEntity> findByChatIdAndSentAtBeforeOrderBySentAtDesc(Long chatId, Instant before);

    Page<MailMessageEntity> findByContactEmail(String contactEmail, Pageable pageable);

    Page<MailMessageEntity> findByContactEmailAndSentAtBefore(String contactEmail, Instant before, Pageable pageable);
}
