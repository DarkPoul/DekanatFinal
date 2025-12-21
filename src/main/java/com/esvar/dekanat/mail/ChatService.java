package com.esvar.dekanat.mail;

import com.esvar.dekanat.mail.dto.AttachmentDto;
import com.esvar.dekanat.mail.dto.ChatListItemDto;
import com.esvar.dekanat.mail.dto.ChatMessageDetailDto;
import com.esvar.dekanat.mail.dto.ChatMessageHeaderDto;
import com.esvar.dekanat.mail.dto.ChatFilter;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final MailMessageRepository mailMessageRepository;
    private final MailAttachmentMetaRepository attachmentRepository;
    private final MailImapClient mailImapClient;
    private final JavaMailSender mailSender;
    private final MailSyncProperties properties;
    private final MailProperties mailProperties;
    private final String defaultFrom;

    public ChatService(ChatRepository chatRepository,
                       MailMessageRepository mailMessageRepository,
                       MailAttachmentMetaRepository attachmentRepository,
                       MailImapClient mailImapClient,
                       JavaMailSender mailSender,
                       MailSyncProperties properties,
                       MailProperties mailProperties,
                       @Value("${mail.default-from:}") String defaultFrom) {
        this.chatRepository = chatRepository;
        this.mailMessageRepository = mailMessageRepository;
        this.attachmentRepository = attachmentRepository;
        this.mailImapClient = mailImapClient;
        this.mailSender = mailSender;
        this.properties = properties;
        this.mailProperties = mailProperties;
        this.defaultFrom = defaultFrom;
    }

    @Transactional(readOnly = true)
    public Page<ChatListItemDto> findChats(ChatFilter filter, Pageable pageable) {
        String query = filter != null && StringUtils.hasText(filter.getQuery()) ? filter.getQuery() : "";
        Sort sort = pageable.getSort().isUnsorted() ? Sort.by(Sort.Direction.DESC, "lastMessageAt") : pageable.getSort();
        Pageable effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Page<ChatEntity> page;
        if (filter != null && filter.getStatuses() != null && !filter.getStatuses().isEmpty()) {
            page = chatRepository.findByPeerEmailContainingIgnoreCaseAndDisplayNameContainingIgnoreCaseAndStatusIn(
                    query, query, filter.getStatuses(), effectivePageable);
        } else {
            page = chatRepository.findByPeerEmailContainingIgnoreCaseAndDisplayNameContainingIgnoreCase(
                    query, query, effectivePageable);
        }
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageHeaderDto> findMessageHeaders(Long chatId, Pageable pageable) {
        Page<MailMessageEntity> page = mailMessageRepository.findByChatId(chatId, pageable);
        return page.map(this::toHeaderDto);
    }

    public void updateStatus(Long chatId, ChatStatus status) {
        ChatEntity chat = chatRepository.findById(chatId).orElseThrow();
        chat.setStatus(status);
        chatRepository.save(chat);
    }

    public void markProcessed(Long chatId) {
        chatRepository.findById(chatId).ifPresent(chat -> {
            chat.setHasUnprocessed(false);
            chatRepository.save(chat);
        });
    }

    @Transactional(readOnly = true)
    public InputStream loadAttachment(String messageId, String attachmentId) throws MessagingException {
        MailAttachmentMetaEntity meta = attachmentRepository.findByMessage_MessageIdAndPartId(messageId, attachmentId);
        if (meta == null) {
            throw new IllegalArgumentException("Attachment not found");
        }
        return loadAttachment(meta);
    }

    @Transactional(readOnly = true)
    public InputStream loadAttachment(Long attachmentMetaId) throws MessagingException {
        MailAttachmentMetaEntity meta = attachmentRepository.findById(attachmentMetaId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
        return loadAttachment(meta);
    }

    private InputStream loadAttachment(MailAttachmentMetaEntity meta) throws MessagingException {
        MailMessageEntity message = meta.getMessage();
        Message mimeMessage = mailImapClient.getMessage(message.getFolder(), message.getUid());
        return MailpartExtractor.extractAttachmentStream(mimeMessage, meta.getPartId());
    }

    public void replyToChat(Long chatId, String body, String subjectOverride) {
        ChatEntity chat = chatRepository.findById(chatId).orElseThrow();
        String to = chat.getPeerEmail();
        Optional<MailMessageEntity> lastMessage = mailMessageRepository.findTop1ByChatIdOrderBySentAtDesc(chatId);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setTo(to);
            helper.setFrom(resolveFromAddress());
            if (lastMessage.isPresent() && StringUtils.hasText(lastMessage.get().getSubject())) {
                helper.setSubject(subjectOverride != null ? subjectOverride : "Re: " + lastMessage.get().getSubject());
            } else {
                helper.setSubject(subjectOverride != null ? subjectOverride : "Re:");
            }
            helper.setText(body, false);
            lastMessage.map(MailMessageEntity::getMessageId).ifPresent(id -> {
                try {
                    mimeMessage.setHeader("In-Reply-To", id);
                } catch (MessagingException e) {
                    throw new RuntimeException(e);
                }
            });
            lastMessage.map(MailMessageEntity::getMessageId).ifPresent(id -> {
                try {
                    mimeMessage.setHeader("References", id);
                } catch (MessagingException e) {
                    throw new RuntimeException(e);
                }
            });
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send reply: " + e.getMessage(), e);
        }
    }

    private String resolveFromAddress() {
        if (StringUtils.hasText(defaultFrom)) {
            return defaultFrom;
        }
        if (StringUtils.hasText(mailProperties.getUsername())) {
            return mailProperties.getUsername();
        }
        return properties.getImap().getUsername();
    }

    private ChatListItemDto toDto(ChatEntity chat) {
        return ChatListItemDto.builder()
                .id(chat.getId())
                .displayName(StringUtils.hasText(chat.getDisplayName()) ? chat.getDisplayName() : "Невідомий")
                .peerEmail(chat.getPeerEmail())
                .orgUnit(chat.getOrgUnit())
                .status(chat.getStatus())
                .hasUnprocessed(chat.isHasUnprocessed())
                .lastMessageAt(chat.getLastMessageAt())
                .build();
    }

    public ChatMessageDetailDto getMessageDetails(Long messageId) throws MessagingException {
        MailMessageEntity entity = mailMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        MailpartExtractor.BodyContent body = ensureContentCached(entity);

        String htmlText = body != null && body.html() != null ? body.html() : "";
        String plainText = body != null && body.plain() != null ? body.plain() : "";

        MailReplySplitter.SplitResult split = MailReplySplitter.split(plainText);
        String replyText = split.reply();
        String quotedText = split.quoted();

        String quotedHtml = "";
        if (StringUtils.hasText(quotedText)) {
            quotedHtml = "<pre style=\"white-space:pre-wrap;\">" +
                    escapeHtml(quotedText) +
                    "</pre>";
        }

        return ChatMessageDetailDto.builder()
                .id(entity.getId())
                .messageId(entity.getMessageId())
                .from(entity.getFromEmail())
                .to(entity.getToEmail())
                .subject(entity.getSubject())
                .bodyHtml(htmlText)
                .bodyText(replyText)
                .quotedText(quotedText)
                .quotedHtml(quotedHtml)
                .snippet(entity.getSnippet())
                .sentAt(entity.getSentAt())
                .direction(entity.getDirection())
                .hasAttachments(entity.isHasAttachments())
                .attachments(entity.getAttachments().stream().map(this::toAttachmentDto).collect(Collectors.toList()))
                .build();
    }

    private ChatMessageHeaderDto toHeaderDto(MailMessageEntity entity) {
        String snippet = entity.getSnippet();
        if (!StringUtils.hasText(snippet) && StringUtils.hasText(entity.getCachedPlainBody())) {
            snippet = MailTextExtractor.sanitizeSnippet(entity.getCachedPlainBody(), 500);
        }

        return ChatMessageHeaderDto.builder()
                .id(entity.getId())
                .messageId(entity.getMessageId())
                .from(entity.getFromEmail())
                .to(entity.getToEmail())
                .subject(entity.getSubject())
                .snippet(snippet)
                .sentAt(entity.getSentAt())
                .direction(entity.getDirection())
                .hasAttachments(entity.isHasAttachments())
                .build();
    }

    private MailpartExtractor.BodyContent ensureContentCached(MailMessageEntity entity) throws MessagingException {
        if (entity.getContentLoadedAt() != null && (StringUtils.hasText(entity.getCachedPlainBody()) || StringUtils.hasText(entity.getCachedHtmlBody()))) {
            return new MailpartExtractor.BodyContent(entity.getCachedHtmlBody(), entity.getCachedPlainBody());
        }

        try {
            Message message = mailImapClient.getMessage(entity.getFolder(), entity.getUid());
            MailpartExtractor.BodyContent body = MailpartExtractor.extractBody(message);

            String html = body != null ? body.html() : null;
            String plain = body != null ? body.plain() : null;

            if (!StringUtils.hasText(html) && !StringUtils.hasText(plain)) {
                Extracted ex = extractTextFallback(message);
                html = ex.html;
                plain = ex.plain;
            }

            String sanitizedHtml = StringUtils.hasText(html) ? MailTextExtractor.sanitizeHtml(html) : "";
            String finalPlain = StringUtils.hasText(plain)
                    ? MailTextExtractor.stripInlinePlaceholders(plain)
                    : (StringUtils.hasText(sanitizedHtml) ? MailTextExtractor.toPlainText(sanitizedHtml) : "");

            if (!StringUtils.hasText(finalPlain) && StringUtils.hasText(entity.getSnippet())) {
                finalPlain = entity.getSnippet();
            }

            persistContent(entity, sanitizedHtml, finalPlain, message);

            return new MailpartExtractor.BodyContent(sanitizedHtml, finalPlain);
        } catch (MessagingException e) {
            if (StringUtils.hasText(entity.getSnippet())) {
                return new MailpartExtractor.BodyContent("", entity.getSnippet());
            }
            throw e;
        } catch (IOException e) {
            throw new MessagingException("Failed to load message content", e);
        }
    }

    private void persistContent(MailMessageEntity entity, String html, String plain, Message message) throws MessagingException, IOException {
        String sanitizedPlain = StringUtils.hasText(plain) ? plain : "";
        entity.setCachedHtmlBody(html);
        entity.setCachedPlainBody(sanitizedPlain);

        if (!StringUtils.hasText(entity.getSnippet())) {
            entity.setSnippet(MailTextExtractor.sanitizeSnippet(sanitizedPlain, 500));
        }

        List<MailAttachmentMetaEntity> attachments = new ArrayList<>();
        collectAttachments(message, "", attachments);
        attachments.forEach(a -> a.setMessage(entity));
        entity.getAttachments().clear();
        entity.getAttachments().addAll(attachments);
        entity.setHasAttachments(!attachments.isEmpty());
        entity.setContentLoadedAt(Instant.now());
        mailMessageRepository.save(entity);
    }

    private void collectAttachments(Part part, String partId, List<MailAttachmentMetaEntity> attachments) throws MessagingException, IOException {
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                Part bodyPart = multipart.getBodyPart(i);
                String childPartId = partId.isEmpty() ? String.valueOf(i + 1) : partId + "." + (i + 1);
                collectAttachments(bodyPart, childPartId, attachments);
            }
            return;
        }

        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || StringUtils.hasText(part.getFileName())) {
            attachments.add(MailAttachmentMetaEntity.builder()
                    .partId(partId.isEmpty() ? "part" : partId)
                    .filename(part.getFileName())
                    .contentType(part.getContentType())
                    .sizeBytes(part.getSize() >= 0 ? (long) part.getSize() : null)
                    .build());
        }
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static class Extracted {
        final String plain;
        final String html;
        Extracted(String plain, String html) { this.plain = plain; this.html = html; }
    }

    private Extracted extractTextFallback(Part part) throws MessagingException {
        try {
            // 1) Базові текстові типи
            if (part.isMimeType("text/plain")) {
                String text = safeText(part);
                return new Extracted(StringUtils.hasText(text) ? text : null, null);
            }
            if (part.isMimeType("text/html")) {
                String html = safeText(part);
                return new Extracted(null, StringUtils.hasText(html) ? html : null);
            }

            // 2) Вкладене повідомлення
            if (part.isMimeType("message/rfc822")) {
                Object c = part.getContent();
                if (c instanceof Part p) return extractTextFallback(p);
                return new Extracted(null, null);
            }

            // 3) Мультипарти
            if (part.isMimeType("multipart/*")) {
                Multipart mp = (Multipart) part.getContent();
                String plain = null;
                String html = null;

                // Спец-випадок: multipart/alternative (plain+html)
                boolean isAlternative = part.isMimeType("multipart/alternative");

                // Спец-випадок: multipart/related (html + inline resources)
                boolean isRelated = part.isMimeType("multipart/related");

                for (int i = 0; i < mp.getCount(); i++) {
                    BodyPart bp = mp.getBodyPart(i);

                    // ❌ Пропускаємо вкладення/ресурси
                    if (shouldSkipBodyPart(bp)) continue;

                    Extracted ex = extractTextFallback(bp);

                    // В alternative: html важливіше, але plain теж треба
                    if (StringUtils.hasText(ex.html) && !StringUtils.hasText(html)) {
                        html = ex.html;
                        if (isAlternative && StringUtils.hasText(plain)) break; // вже є і plain і html
                    }
                    if (StringUtils.hasText(ex.plain) && !StringUtils.hasText(plain)) {
                        plain = ex.plain;
                        if (isAlternative && StringUtils.hasText(html)) break;
                    }

                    // У related часто достатньо лише html (plain може не існувати)
                    if (isRelated && StringUtils.hasText(html)) {
                        // але не break якщо хочеш ще plain, тут зазвичай можна break
                        // break;
                    }
                }

                return new Extracted(plain, html);
            }

            // Інші mime-типы ігноруємо
            return new Extracted(null, null);

        } catch (Exception ex) {
            throw new MessagingException("Failed to extract body fallback", ex);
        }
    }

    private boolean shouldSkipBodyPart(BodyPart bp) throws MessagingException {
        String disp = bp.getDisposition();
        String fileName = bp.getFileName();
        String ct = bp.getContentType() != null ? bp.getContentType().toLowerCase() : "";

        // 1) Явні вкладення
        if (disp != null && disp.equalsIgnoreCase(Part.ATTACHMENT)) return true;

        // 2) Inline з filename — дуже часто це теж “вкладення”
        if (disp != null && disp.equalsIgnoreCase(Part.INLINE) && StringUtils.hasText(fileName)) return true;

        // 3) Inline картинки / ресурси в multipart/related
        if (disp != null && disp.equalsIgnoreCase(Part.INLINE) && ct.startsWith("image/")) return true;

        // 4) Часто зустрічається: application/* як частина листа — це не body
        if (ct.startsWith("application/")) return true;

        // 5) Іноді “вкладення” без disposition але з filename
        if (StringUtils.hasText(fileName) && !bp.isMimeType("text/*")) return true;

        return false;
    }

    private String safeText(Part part) throws Exception {
        Object c = part.getContent();
        if (c == null) return null;

        // JavaMail може повернути String або InputStream залежно від декодування
        if (c instanceof String s) return s;

        // Якщо раптом повернув stream — читаємо обережно
        if (c instanceof java.io.InputStream is) {
            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }

        return c.toString();
    }

    private AttachmentDto toAttachmentDto(MailAttachmentMetaEntity attachment) {
        return AttachmentDto.builder()
                .id(attachment.getId())
                .attachmentId(attachment.getPartId())
                .filename(attachment.getFilename())
                .sizeBytes(attachment.getSizeBytes())
                .mimeType(attachment.getContentType())
                .build();
    }
}
