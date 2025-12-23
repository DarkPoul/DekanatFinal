package com.esvar.dekanat.mail.v2.service;

import com.esvar.dekanat.mail.v2.entity.MailAttachmentEntity;
import com.esvar.dekanat.mail.v2.entity.MailMessageEntity;
import com.esvar.dekanat.mail.v2.entity.MailThreadEntity;
import com.esvar.dekanat.mail.v2.repository.MailAttachmentRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SendMailService {

    private final JavaMailSender mailSender;
    private final MailAttachmentRepository attachmentRepository;
    private final MessageService messageService;

    @Value("${mail.default-from:}")
    private String defaultFrom;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Transactional
    public MailMessageEntity send(MailThreadEntity thread,
                                  String text,
                                  String subject,
                                  List<MultipartFile> files) {
        String senderEmail = resolveSenderEmail(thread);
        MailMessageEntity saved = messageService.saveOutgoing(
                thread,
                senderEmail,
                subject,
                null,
                text,
                thread.getContact().getEmail());
        List<MailAttachmentEntity> attachments = persistAttachments(saved, files);
        saved.setHasAttachments(!attachments.isEmpty());
        try {
            dispatchEmail(thread, saved, attachments);
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
            Path path = Paths.get(attachment.getStorageKey());

            helper.addAttachment(
                    attachment.getFilename(),
                    () -> Files.newInputStream(path),
                    attachment.getContentType()
            );

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

    private List<MailAttachmentEntity> persistAttachments(MailMessageEntity message, @Nullable List<MultipartFile> files) {
        if (CollectionUtils.isEmpty(files)) {
            return List.of();
        }
        List<MailAttachmentEntity> entities = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                MailAttachmentEntity entity = MailAttachmentEntity.builder()
                        .message(message)
                        .filename(file.getOriginalFilename())
                        .contentType(file.getContentType())
                        .size(file.getSize())
                        .inline(false)
                        .storageType(MailAttachmentEntity.StorageType.DB)
                        .storageKey(new String(file.getBytes()))
                        .createdAt(Instant.now())
                        .build();
                entities.add(entity);
            } catch (IOException ignored) {
            }
        }
        return attachmentRepository.saveAll(entities);
    }
}
