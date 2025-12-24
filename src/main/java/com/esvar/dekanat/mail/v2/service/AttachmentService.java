package com.esvar.dekanat.mail.v2.service;

import com.esvar.dekanat.mail.v2.entity.MailAttachmentEntity;
import com.esvar.dekanat.mail.v2.repository.MailAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Locale;
import java.util.Optional;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private final MailAttachmentRepository attachmentRepository;

    public Optional<AttachmentContent> loadAttachment(Long id) {
        Optional<MailAttachmentEntity> attachmentOpt = attachmentRepository.findById(id);
        if (attachmentOpt.isEmpty()) {
            log.warn("Attachment {} not found while attempting to download", id);
            return Optional.empty();
        }

        MailAttachmentEntity attachment = attachmentOpt.get();
        log.debug("Attachment {} fetched for download: filename={}, contentType={}, storageType={}, storageKeyLength={}",
                id,
                attachment.getFilename(),
                attachment.getContentType(),
                attachment.getStorageType(),
                StringUtils.hasText(attachment.getStorageKey()) ? attachment.getStorageKey().length() : 0);

        return Optional.of(attachment)
                .map(this::toAttachmentContent)
                .flatMap(content -> filterExistingResource(content, attachment));
    }

    public Optional<AttachmentContent> loadInline(Long id) {
        Optional<MailAttachmentEntity> attachmentOpt = attachmentRepository.findById(id);
        if (attachmentOpt.isEmpty()) {
            log.warn("Inline attachment {} not found", id);
            return Optional.empty();
        }

        MailAttachmentEntity attachment = attachmentOpt.get();
        if (!isRenderableInline(attachment)) {
            log.warn("Attachment {} is not renderable inline. filename={}, contentType={}, inline={}, contentId={}",
                    id,
                    attachment.getFilename(),
                    attachment.getContentType(),
                    attachment.isInline(),
                    attachment.getContentId());
            return Optional.empty();
        }

        log.debug("Attachment {} prepared for inline rendering: filename={}, contentType={}, storageType={}, storageKeyLength={}",
                id,
                attachment.getFilename(),
                attachment.getContentType(),
                attachment.getStorageType(),
                StringUtils.hasText(attachment.getStorageKey()) ? attachment.getStorageKey().length() : 0);

        return Optional.of(attachment)
                .map(this::toAttachmentContent)
                .flatMap(content -> filterExistingResource(content, attachment));
    }

    public MediaType resolveMediaType(AttachmentContent content) {
        MediaType parsed = parseMediaType(content.contentType());
        if (parsed != null) {
            return parsed;
        }
        return MediaTypeFactory.getMediaType(content.filename())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    private boolean isRenderableInline(MailAttachmentEntity attachment) {
        if (attachment.isInline() || StringUtils.hasText(attachment.getContentId())) {
            return true;
        }
        return isImageContent(attachment.getContentType(), attachment.getFilename());
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
            log.debug("Attachment {} uses filesystem storage: {}", entity.getId(), path);
            return new FileSystemResource(path);
        }
        byte[] bytes = decodeStorageKey(entity.getStorageKey());
        log.debug("Attachment {} uses encoded storage, decoded {} bytes", entity.getId(), bytes.length);
        if (bytes.length == 0) {
            Resource fileResource = fallbackToFileResource(entity.getStorageKey());
            if (fileResource != null) {
                log.debug("Attachment {} storageKey could not be decoded; using fallback file resource", entity.getId());
                return fileResource;
            }
            log.warn("Attachment {} storageKey decoded to empty bytes and no fallback resource found", entity.getId());
        }
        return new ByteArrayResource(bytes);
    }

    private Optional<AttachmentContent> filterExistingResource(AttachmentContent content, MailAttachmentEntity attachment) {
        Resource resource = content.resource();
        if (resource == null) {
            log.warn("Attachment {} produced null resource", attachment.getId());
            return Optional.empty();
        }
        try {
            if (resource.exists() && resource.contentLength() > 0) {
                return Optional.of(content);
            }
        } catch (IOException e) {
            log.error("Failed to read attachment {} resource metadata", attachment.getId(), e);
        }
        log.warn("Attachment {} resource missing or empty. storageType={}, storageKeyLength={}, filename={}, contentType={}",
                attachment.getId(),
                attachment.getStorageType(),
                StringUtils.hasText(attachment.getStorageKey()) ? attachment.getStorageKey().length() : 0,
                attachment.getFilename(),
                attachment.getContentType());
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

    private boolean isImageContent(String contentType, String filename) {
        if (StringUtils.hasText(contentType) && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        if (!StringUtils.hasText(filename)) {
            return false;
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".gif")
                || lower.endsWith(".bmp")
                || lower.endsWith(".webp")
                || lower.endsWith(".heic")
                || lower.endsWith(".heif");
    }

    public record AttachmentContent(String filename, String contentType, Resource resource) {
    }
}
