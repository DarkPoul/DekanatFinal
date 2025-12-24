package com.esvar.dekanat.mail.v2.service;

import com.esvar.dekanat.mail.v2.entity.MailAttachmentEntity;
import com.esvar.dekanat.mail.v2.entity.MailMessageEntity;
import com.esvar.dekanat.mail.v2.entity.MailThreadEntity;
import com.esvar.dekanat.mail.v2.repository.MailAttachmentRepository;
import com.esvar.dekanat.mail.v2.repository.MailThreadRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SendMailService {

    private final JavaMailSender mailSender;
    private final MailAttachmentRepository attachmentRepository;
    private final MailThreadRepository threadRepository;
    private final MessageService messageService;
    private final AttachmentStorageService attachmentStorageService;

    @Value("${mail.default-from:}")
    private String defaultFrom;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Transactional
    public MailMessageEntity send(MailThreadEntity thread,
                                  String text,
                                  String subject,
                                  List<MultipartFile> files) {
        MailThreadEntity managedThread = threadRepository.findWithContact(thread.getId())
                .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + thread.getId()));
        String senderEmail = resolveSenderEmail(managedThread);
        MailMessageEntity saved = messageService.saveOutgoing(
                managedThread,
                senderEmail,
                subject,
                null,
                text,
                managedThread.getContact().getEmail());
        List<MailAttachmentEntity> attachments = persistAttachments(saved, files);
        saved.setHasAttachments(!attachments.isEmpty());
        try {
            dispatchEmail(managedThread, saved, attachments);
        } catch (MessagingException | IOException e) {
            // For demo purposes we skip failing the transaction to keep UI responsive.
        }
        return saved;
    }

    private void dispatchEmail(MailThreadEntity thread,
                               MailMessageEntity message,
                               List<MailAttachmentEntity> attachments) throws MessagingException, IOException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setTo(thread.getContact().getEmail());
        helper.setSubject(message.getSubject());
        helper.setText(message.getBodyText(), false);
        helper.setFrom(StringUtils.hasText(message.getFromEmail()) ? message.getFromEmail() : resolveSenderEmail(thread));
        for (MailAttachmentEntity attachment : attachments) {
            Resource resource = resolveAttachmentResource(attachment);
            if (resource != null) {
                helper.addAttachment(
                        attachment.getFilename(),
                        resource,
                        attachment.getContentType()
                );
            }
        }
        mailSender.send(mimeMessage);
    }

    private String resolveSenderEmail(MailThreadEntity thread) {
        if (StringUtils.hasText(defaultFrom)) {
            return defaultFrom;
        }
        if (StringUtils.hasText(mailUsername)) {
            return mailUsername;
        }
        return thread.getContact().getEmail();
    }

    private Resource resolveAttachmentResource(MailAttachmentEntity attachment) {
        if (attachment.getStorageType() == MailAttachmentEntity.StorageType.FS
                && StringUtils.hasText(attachment.getStorageKey())) {
            return new FileSystemResource(attachment.getStorageKey());
        }
        byte[] bytes = decodeStorageKey(attachment.getStorageKey());
        return new ByteArrayResource(bytes);
    }

    private byte[] decodeStorageKey(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            return new byte[0];
        }
        try {
            return java.util.Base64.getDecoder().decode(storageKey);
        } catch (IllegalArgumentException ignored) {
            return storageKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private List<MailAttachmentEntity> persistAttachments(MailMessageEntity message, @Nullable List<MultipartFile> files) {
        if (CollectionUtils.isEmpty(files)) {
            return List.of();
        }
        List<MailAttachmentEntity> entities = new ArrayList<>();
        for (MultipartFile file : files) {
            AttachmentStorageService.StoredAttachment storedAttachment = attachmentStorageService.storeImage(file);
            MailAttachmentEntity entity = MailAttachmentEntity.builder()
                    .message(message)
                    .filename(storedAttachment.originalFilename())
                    .contentType(storedAttachment.contentType())
                    .size(storedAttachment.size())
                    .inline(false)
                    .storageType(MailAttachmentEntity.StorageType.FS)
                    .storageKey(storedAttachment.storagePath())
                    .createdAt(Instant.now())
                    .build();
            entities.add(entity);
        }
        return attachmentRepository.saveAll(entities);
    }
}
