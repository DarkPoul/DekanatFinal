package com.esvar.dekanat.mail.v2.repository;

import com.esvar.dekanat.mail.v2.entity.MailMessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MailMessageRepository extends JpaRepository<MailMessageEntity, Long> {

    List<MailMessageEntity> findByThreadIdOrderBySentAtDesc(Long threadId, Pageable pageable);

    List<MailMessageEntity> findByThreadIdAndSentAtBeforeOrderBySentAtDesc(Long threadId, Instant before, Pageable pageable);

    Optional<MailMessageEntity> findTop1ByThreadIdAndDirectionOrderBySentAtDesc(Long threadId, MailMessageEntity.Direction direction);

    @Query("select m from MailMessageEntity m where m.thread.id = :threadId order by m.sentAt desc")
    List<MailMessageEntity> findPaged(@Param("threadId") Long threadId, Pageable pageable);

    Optional<MailMessageEntity> findByExternalId(String externalId);
}
