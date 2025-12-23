package com.esvar.dekanat.mail.v2.service;

import com.esvar.dekanat.mail.v2.entity.MailAttachmentEntity;
import com.esvar.dekanat.mail.v2.repository.MailAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final MailAttachmentRepository attachmentRepository;

    public Optional<AttachmentContent> loadAttachment(Long id) {
        return attachmentRepository.findById(id)
                .map(this::toAttachmentContent);
    }

    public Optional<AttachmentContent> loadInline(Long id) {
        return attachmentRepository.findByIdAndInlineTrue(id)
                .map(this::toAttachmentContent);
    }

    private AttachmentContent toAttachmentContent(MailAttachmentEntity entity) {
        return new AttachmentContent(
                entity.getFilename(),
                entity.getContentType(),
                toResource(entity)
        );
    }

    private Resource toResource(MailAttachmentEntity entity) {
        if (entity.getStorageType() == MailAttachmentEntity.StorageType.FS
                && StringUtils.hasText(entity.getStorageKey())) {
            Path path = Paths.get(entity.getStorageKey());
            return new FileSystemResource(path);
        }
        byte[] bytes = decodeStorageKey(entity.getStorageKey());
        return new ByteArrayResource(bytes);
    }

    byte[] decodeStorageKey(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            return new byte[0];
        }
        try {
            return Base64.getDecoder().decode(storageKey);
        } catch (IllegalArgumentException ignored) {
            return storageKey.getBytes(StandardCharsets.UTF_8);
        }
    }

    public record AttachmentContent(String filename, String contentType, Resource resource) {
    }
}
