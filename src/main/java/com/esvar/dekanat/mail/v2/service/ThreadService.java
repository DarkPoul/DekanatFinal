package com.esvar.dekanat.mail.v2.service;

import com.esvar.dekanat.mail.v2.dto.ThreadListItemDto;
import com.esvar.dekanat.mail.v2.entity.MailContactEntity;
import com.esvar.dekanat.mail.v2.entity.MailMessageEntity;
import com.esvar.dekanat.mail.v2.entity.MailThreadEntity;
import com.esvar.dekanat.mail.v2.repository.MailContactRepository;
import com.esvar.dekanat.mail.v2.repository.MailMessageRepository;
import com.esvar.dekanat.mail.v2.repository.MailThreadRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ThreadService {

    private final MailThreadRepository threadRepository;
    private final MailContactRepository contactRepository;
    private final MailMessageRepository messageRepository;

    public Page<ThreadListItemDto> findThreads(String nameQuery,
                                               String emailQuery,
                                               String orgQuery,
                                               MailThreadEntity.ThreadStatus status,
                                               int offset,
                                               int limit) {
        int pageIndex = Math.floorDiv(offset, limit);
        Pageable pageable = PageRequest.of(pageIndex, limit, Sort.by(Sort.Direction.DESC, "lastIncomingAt"));
        Page<MailThreadEntity> threadsPage = threadRepository.search(trimToNull(nameQuery), trimToNull(emailQuery), trimToNull(orgQuery), status, pageable);
        return threadsPage.map(this::toDto);
    }

    public long countThreads(String nameQuery,
                             String emailQuery,
                             String orgQuery,
                             MailThreadEntity.ThreadStatus status) {
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "lastIncomingAt"));
        return threadRepository.search(trimToNull(nameQuery), trimToNull(emailQuery), trimToNull(orgQuery), status, pageable)
                .getTotalElements();
    }

    public Optional<MailThreadEntity> findById(Long id) {
        return threadRepository.findById(id);
    }

    @Transactional
    public void updateStatus(Long threadId, MailThreadEntity.ThreadStatus status) {
        threadRepository.findById(threadId).ifPresent(thread -> {
            thread.setStatus(status);
            threadRepository.save(thread);
        });
    }

    @Transactional
    public void markViewed(Long threadId) {
        threadRepository.findById(threadId).ifPresent(thread -> {
            thread.setLastViewedAt(Instant.now());
            thread.setUnreadIncomingCount(0);
        });
    }

    @Transactional
    public void signContact(Long contactId, String displayName, String orgUnitText) {
        contactRepository.findById(contactId).ifPresent(contact -> {
            contact.setDisplayName(displayName);
            if (StringUtils.hasText(orgUnitText)) {
                contact.setOrgUnitText(orgUnitText);
            }
            contact.setUpdatedAt(Instant.now());
        });
    }

    private ThreadListItemDto toDto(MailThreadEntity thread) {
        MailContactEntity contact = thread.getContact();
        List<MailMessageEntity> latestMessage = messageRepository.findPaged(thread.getId(), PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "sentAt")));
        String lastSubject = latestMessage.stream()
                .findFirst()
                .map(MailMessageEntity::getSubject)
                .map(SubjectNormalizer::normalize)
                .orElse(null);
        return ThreadListItemDto.builder()
                .threadId(thread.getId())
                .contactId(contact.getId())
                .displayName(StringUtils.hasText(contact.getDisplayName()) ? contact.getDisplayName() : "Невідомий")
                .email(contact.getEmail())
                .orgUnitText(StringUtils.hasText(contact.getOrgUnitText()) ? contact.getOrgUnitText() : "Невідомий")
                .lastSubject(lastSubject)
                .lastIncomingAt(thread.getLastIncomingAt())
                .status(thread.getStatus())
                .unreadIncomingCount(thread.getUnreadIncomingCount())
                .external(contact.getType() == MailContactEntity.ContactType.EXTERNAL)
                .signed(StringUtils.hasText(contact.getDisplayName()))
                .build();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
