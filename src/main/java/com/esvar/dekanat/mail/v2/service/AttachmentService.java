package com.esvar.dekanat.mail.v2.service;

import com.esvar.dekanat.mail.v2.entity.MailAttachmentEntity;
import com.esvar.dekanat.mail.v2.repository.MailAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final MailAttachmentRepository attachmentRepository;

    public Optional<AttachmentContent> loadAttachment(Long id) {
        return attachmentRepository.findById(id).map(entity -> new AttachmentContent(
                entity.getFilename(),
                entity.getContentType(),
                toResource(entity.getStorageKey())
        ));
    }

    public Optional<AttachmentContent> loadInline(Long id) {
        return attachmentRepository.findByIdAndInlineTrue(id).map(entity -> new AttachmentContent(
                entity.getFilename(),
                entity.getContentType(),
                toResource(entity.getStorageKey())
        ));
    }

    private Resource toResource(String storageKey) {
        // Placeholder storage implementation uses in-memory content for demo purposes.
        byte[] bytes = storageKey != null ? storageKey.getBytes(StandardCharsets.UTF_8) : new byte[0];
        return new ByteArrayResource(bytes);
    }

    public record AttachmentContent(String filename, String contentType, Resource resource) {
    }
}
