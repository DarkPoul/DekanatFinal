package com.esvar.dekanat.mail;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public final class MailpartExtractor {

    private MailpartExtractor() {
    }

    public static String extractPlainText(Message message) {
        try {
            return extractText(message, "");
        } catch (Exception e) {
            return "";
        }
    }

    public static InputStream extractAttachmentStream(Message message, String partId) {
        try {
            return findAttachment(message, partId, "").orElseThrow(() -> new IllegalArgumentException("Attachment not found")).getInputStream();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load attachment: " + e.getMessage(), e);
        }
    }

    public static BodyContent extractBody(Message message) {
        BodyCollector collector = new BodyCollector();
        try {
            collectBody(message, collector);
        } catch (Exception e) {
            return new BodyContent("", "");
        }
        return new BodyContent(collector.html, collector.plain);
    }

    public static InlineContent extractInlineContent(Message message, String contentId) {
        try {
            Part part = findInlinePart(message, normalizeContentId(contentId), "");
            if (part == null) {
                return null;
            }
            return new InlineContent(part.getInputStream(), part.getContentType());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load inline resource: " + e.getMessage(), e);
        }
    }

    public static String extractContentId(Part part) throws MessagingException {
        String[] ids = part.getHeader("Content-ID");
        if (ids == null || ids.length == 0) {
            return null;
        }
        return normalizeContentId(ids[0]);
    }

    private static Optional<Part> findAttachment(Part part, String targetPartId, String currentPartId) throws MessagingException, IOException {
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String childPartId = currentPartId.isEmpty() ? String.valueOf(i + 1) : currentPartId + "." + (i + 1);
                Optional<Part> match = findAttachment(bodyPart, targetPartId, childPartId);
                if (match.isPresent()) {
                    return match;
                }
            }
            return Optional.empty();
        }
        if (targetPartId.equals(currentPartId) || (currentPartId.isEmpty() && "part".equals(targetPartId))) {
            if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || StringUtils.hasText(part.getFileName())) {
                return Optional.of(part);
            }
        }
        return Optional.empty();
    }

    private static Part findInlinePart(Part part, String targetContentId, String currentPartId) throws MessagingException, IOException {
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String childPartId = currentPartId.isEmpty() ? String.valueOf(i + 1) : currentPartId + "." + (i + 1);
                Part match = findInlinePart(bodyPart, targetContentId, childPartId);
                if (match != null) {
                    return match;
                }
            }
            return null;
        }
        String contentId = extractContentId(part);
        if (StringUtils.hasText(targetContentId) && targetContentId.equalsIgnoreCase(contentId)) {
            return part;
        }
        return null;
    }

    private static String extractText(Part part, String partId) throws MessagingException, IOException {
        if (part.isMimeType("text/plain")) {
            return part.getContent().toString();
        }
        if (part.isMimeType("text/html")) {
            return MailTextExtractor.toPlainText(part.getContent().toString());
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String childPartId = partId.isEmpty() ? String.valueOf(i + 1) : partId + "." + (i + 1);
                String nested = extractText(bodyPart, childPartId);
                if (StringUtils.hasText(nested)) {
                    if (builder.length() > 0) {
                        builder.append("\n\n");
                    }
                    builder.append(nested);
                }
            }
            return builder.toString();
        }
        return "";
    }

    private static void collectBody(Part part, BodyCollector collector) throws MessagingException, IOException {
        if (part.isMimeType("text/html")) {
            if (!StringUtils.hasText(collector.html)) {
                collector.html = part.getContent().toString();
            }
            if (!StringUtils.hasText(collector.plain)) {
                collector.plain = MailTextExtractor.toPlainText(collector.html);
            }
            return;
        }
        if (part.isMimeType("text/plain")) {
            if (!StringUtils.hasText(collector.plain)) {
                collector.plain = part.getContent().toString();
            }
            return;
        }
        if (part.isMimeType("multipart/alternative")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = multipart.getCount() - 1; i >= 0; i--) {
                collectBody(multipart.getBodyPart(i), collector);
            }
            return;
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                collectBody(multipart.getBodyPart(i), collector);
            }
        }
    }

    private static final class BodyCollector {
        private String html = "";
        private String plain = "";
    }

    public record BodyContent(String html, String plain) {
    }

    public record InlineContent(InputStream stream, String contentType) {
    }

    private static String normalizeContentId(String contentId) {
        if (contentId == null) {
            return null;
        }
        String cleaned = contentId.trim();
        if (cleaned.startsWith("<") && cleaned.endsWith(">")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        if (cleaned.startsWith("cid:")) {
            cleaned = cleaned.substring(4);
        }
        return cleaned;
    }
}
