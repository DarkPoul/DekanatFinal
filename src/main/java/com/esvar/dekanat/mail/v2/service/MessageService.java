package com.esvar.dekanat.mail.v2.service;

import com.esvar.dekanat.mail.v2.dto.MessageDto;
import com.esvar.dekanat.mail.v2.entity.MailMessageEntity;
import com.esvar.dekanat.mail.v2.entity.MailThreadEntity;
import com.esvar.dekanat.mail.v2.repository.MailAttachmentRepository;
import com.esvar.dekanat.mail.v2.repository.MailMessageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MailMessageRepository messageRepository;
    private final MailAttachmentRepository attachmentRepository;

    public List<MessageDto> loadMessages(Long threadId, Instant before, int limit) {
        List<MailMessageEntity> messages = before == null
                ? messageRepository.findByThreadIdOrderBySentAtDesc(threadId, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "sentAt")))
                : messageRepository.findByThreadIdAndSentAtBeforeOrderBySentAtDesc(threadId, before, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "sentAt")));
        Collections.reverse(messages);
        return messages.stream()
                .map(message -> MessageDto.fromEntity(message, attachmentRepository.findByMessageId(message.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public MailMessageEntity saveOutgoing(MailThreadEntity thread,
                                          String fromEmail,
                                          String subject,
                                          String bodyHtml,
                                          String bodyText,
                                          String toEmail) {
        MailMessageEntity entity = MailMessageEntity.builder()
                .thread(thread)
                .direction(MailMessageEntity.Direction.OUT)
                .fromEmail(fromEmail)
                .toEmail(toEmail)
                .subject(subject)
                .sentAt(Instant.now())
                .bodyHtml(bodyHtml)
                .bodyText(bodyText)
                .hasAttachments(false)
                .build();
        return messageRepository.save(entity);
    }
}
