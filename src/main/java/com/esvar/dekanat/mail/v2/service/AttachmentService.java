package com.esvar.dekanat.mail.v2.service;

import com.esvar.dekanat.mail.v2.entity.MailAttachmentEntity;
import com.esvar.dekanat.mail.v2.repository.MailAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
                .map(this::toAttachmentContent)
                .flatMap(this::filterExistingResource);
    }

    public Optional<AttachmentContent> loadInline(Long id) {
        return attachmentRepository.findByIdAndInlineTrue(id)
                .or(() -> attachmentRepository.findById(id)
                        .filter(attachment -> StringUtils.hasText(attachment.getContentId())))
                .map(this::toAttachmentContent)
                .flatMap(this::filterExistingResource);
    }

    public MediaType resolveMediaType(AttachmentContent content) {
        MediaType parsed = parseMediaType(content.contentType());
        if (parsed != null) {
            return parsed;
        }
        return MediaTypeFactory.getMediaType(content.filename())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
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
        if (bytes.length == 0) {
            Resource fileResource = fallbackToFileResource(entity.getStorageKey());
            if (fileResource != null) {
                return fileResource;
            }
        }
        return new ByteArrayResource(bytes);
    }

    private Optional<AttachmentContent> filterExistingResource(AttachmentContent content) {
        Resource resource = content.resource();
        if (resource == null) {
            return Optional.empty();
        }
        try {
            if (resource.exists() && resource.contentLength() > 0) {
                return Optional.of(content);
            }
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }

    byte[] decodeStorageKey(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            return new byte[0];
        }
        try {
            return Base64.getDecoder().decode(storageKey);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            return Base64.getMimeDecoder().decode(storageKey);
        } catch (IllegalArgumentException ignored) {
        }
        return storageKey.getBytes(StandardCharsets.UTF_8);
    }

    private MediaType parseMediaType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return null;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException ignored) {
            return null;
        }
    }

    private Resource fallbackToFileResource(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            return null;
        }
        Path candidate = Paths.get(storageKey);
        if (!Files.exists(candidate)) {
            return null;
        }
        try {
            if (Files.isReadable(candidate) && Files.size(candidate) > 0) {
                return new FileSystemResource(candidate);
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    public record AttachmentContent(String filename, String contentType, Resource resource) {
    }
}
