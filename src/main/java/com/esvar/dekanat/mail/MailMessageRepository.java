package com.esvar.dekanat.mail;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MailMessageRepository extends JpaRepository<MailMessageEntity, Long> {

    Optional<MailMessageEntity> findByMessageId(String messageId);

    Page<MailMessageEntity> findByChatId(Long chatId, Pageable pageable);

    Optional<MailMessageEntity> findTop1ByChatIdOrderBySentAtDesc(Long chatId);
}
