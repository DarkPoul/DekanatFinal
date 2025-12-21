package com.esvar.dekanat.mail;

import com.esvar.dekanat.mail.dto.AttachmentDto;
import com.esvar.dekanat.mail.dto.ChatFilter;
import com.esvar.dekanat.mail.dto.ChatListItemDto;
import com.esvar.dekanat.mail.dto.ChatMessageDetailDto;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    public record AttachmentContent(InputStream stream, String contentType, String filename) {
    }

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
        List<ChatStatus> statuses = filter != null && filter.getStatuses() != null ? filter.getStatuses() : List.of();
        boolean statusesEmpty = statuses.isEmpty();
        Page<ChatEntity> page = chatRepository.search(query, statuses, statusesEmpty, effectivePageable);
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDetailDto> findChatMessages(Long chatId, Instant before) throws MessagingException {
        if (chatId == null) {
            return List.of();
        }
        List<MailMessageEntity> messages = before != null
                ? mailMessageRepository.findByChatIdAndSentAtBeforeOrderBySentAtDesc(chatId, before)
                : mailMessageRepository.findByChatIdOrderBySentAtDesc(chatId);

        if (messages.isEmpty()) {
            Optional<ChatEntity> chat = chatRepository.findById(chatId);
            if (chat.isPresent() && StringUtils.hasText(chat.get().getContactEmail())) {
                String normalized = chat.get().getContactEmail().trim().toLowerCase();
                messages = before != null
                        ? mailMessageRepository.findByContactEmailAndSentAtBeforeOrderBySentAtDesc(normalized, before)
                        : mailMessageRepository.findByContactEmailOrderBySentAtDesc(normalized);
            }
        }
        List<ChatMessageDetailDto> details = new ArrayList<>();
        for (MailMessageEntity message : messages) {
            details.add(toDetailDto(message));
        }
        return details;
    }

    @Transactional
    public ChatMessageDetailDto getMessageDetails(Long messageId) throws MessagingException {
        MailMessageEntity entity = mailMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        return toDetailDto(entity);
    }

    public void updateStatus(Long chatId, ChatStatus status) {
        chatRepository.findById(chatId).ifPresent(chat -> {
            chat.setStatus(status);
            chat.setHasUnprocessed(false);
            chatRepository.save(chat);
        });
    }

    public void markProcessed(Long chatId) {
        chatRepository.findById(chatId).ifPresent(chat -> {
            chat.setHasUnprocessed(false);
            chat.setUnreadCount(0);
            chatRepository.save(chat);
        });
    }

    @Transactional(readOnly = true)
    public AttachmentContent loadAttachment(String messageId, String attachmentId) throws MessagingException {
        MailAttachmentMetaEntity meta = attachmentRepository.findByMessage_MessageIdAndPartId(messageId, attachmentId);
        if (meta != null) {
            return loadAttachment(meta);
        }
        MailMessageEntity entity = mailMessageRepository.findByMessageId(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        Message mimeMessage = mailImapClient.getMessage(entity.getFolder(), entity.getUid());
        List<MailAttachmentMetaEntity> transientAttachments = collectTransientAttachments(mimeMessage, "", entity);
        MailAttachmentMetaEntity transientMeta = transientAttachments.stream()
                .filter(a -> attachmentId.equals(a.getPartId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
        InputStream stream = MailpartExtractor.extractAttachmentStream(mimeMessage, attachmentId);
        return new AttachmentContent(stream, transientMeta.getContentType(), transientMeta.getFilename());
    }

    @Transactional(readOnly = true)
    public AttachmentContent loadAttachment(Long attachmentMetaId) throws MessagingException {
        MailAttachmentMetaEntity meta = attachmentRepository.findById(attachmentMetaId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
        return loadAttachment(meta);
    }

    @Transactional(readOnly = true)
    public AttachmentContent loadInline(Long messageId, String contentId) throws MessagingException {
        MailMessageEntity entity = mailMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        String normalizedCid = normalizeContentId(contentId);
        MailAttachmentMetaEntity meta = attachmentRepository.findByMessage_IdAndContentId(messageId, normalizedCid);
        Message mimeMessage = mailImapClient.getMessage(entity.getFolder(), entity.getUid());
        MailpartExtractor.InlineContent inlineContent = MailpartExtractor.extractInlineContent(mimeMessage, normalizedCid);
        if (inlineContent == null) {
            throw new IllegalArgumentException("Inline attachment not found");
        }
        String contentType = inlineContent.contentType() != null
                ? inlineContent.contentType()
                : (meta != null ? meta.getContentType() : "application/octet-stream");
        String filename = meta != null ? meta.getFilename() : null;
        return new AttachmentContent(inlineContent.stream(), contentType, filename);
    }

    private AttachmentContent loadAttachment(MailAttachmentMetaEntity meta) throws MessagingException {
        MailMessageEntity message = meta.getMessage();
        Message mimeMessage = mailImapClient.getMessage(message.getFolder(), message.getUid());
        InputStream stream = MailpartExtractor.extractAttachmentStream(mimeMessage, meta.getPartId());
        return new AttachmentContent(stream, meta.getContentType(), meta.getFilename());
    }

    public void replyToChat(Long chatId, String body, String subjectOverride) {
        ChatEntity chat = chatRepository.findById(chatId).orElseThrow();
        String to = chat.getContactEmail();
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
        String contact = StringUtils.hasText(chat.getContactEmail()) ? chat.getContactEmail() : chat.getPeerEmail();
        String display = StringUtils.hasText(chat.getDisplayName()) ? chat.getDisplayName() : contact;
        return ChatListItemDto.builder()
                .id(chat.getId())
                .contactEmail(contact)
                .displayName(display)
                .status(chat.getStatus())
                .hasUnprocessed(chat.isHasUnprocessed())
                .unreadCount(chat.getUnreadCount())
                .lastMessageAt(chat.getLastMessageAt())
                .lastSnippet(chat.getLastSnippet())
                .hasAttachments(chat.isHasAttachments())
                .build();
    }

    private ChatMessageDetailDto toDetailDto(MailMessageEntity entity) throws MessagingException {
        ContentPayload content = ensureContentPayload(entity);

        String htmlText = content.html();
        String plainText = content.plain();
        if (!StringUtils.hasText(plainText) && StringUtils.hasText(htmlText)) {
            plainText = MailTextExtractor.toPlainText(htmlText);
        }

        String bodyTextClean = MailQuotedStripper.stripQuotedPlain(plainText);
        String sanitizedHtml = StringUtils.hasText(htmlText) ? MailTextExtractor.sanitizeHtml(htmlText) : "";
        String bodyHtmlClean = MailQuotedStripper.stripQuotedHtml(sanitizedHtml);

        if (!StringUtils.hasText(bodyTextClean) && StringUtils.hasText(bodyHtmlClean)) {
            bodyTextClean = MailTextExtractor.toPlainText(bodyHtmlClean);
        }

        String htmlWithInline = rewriteInlineCid(sanitizedHtml, entity.getId());
        String cleanHtmlWithInline = rewriteInlineCid(bodyHtmlClean, entity.getId());
        if (!StringUtils.hasText(cleanHtmlWithInline) && StringUtils.hasText(bodyTextClean)) {
            cleanHtmlWithInline = toSimpleHtml(bodyTextClean);
        }

        String snippet = StringUtils.hasText(content.snippet())
                ? content.snippet()
                : MailTextExtractor.sanitizeSnippet(bodyTextClean, 500);

        return ChatMessageDetailDto.builder()
                .id(entity.getId())
                .messageId(entity.getMessageId())
                .from(entity.getFromEmail())
                .to(entity.getToEmail())
                .subject(entity.getSubject())
                .bodyHtml(htmlWithInline)
                .bodyHtmlClean(cleanHtmlWithInline)
                .bodyText(plainText)
                .bodyTextClean(bodyTextClean)
                .quotedText("")
                .quotedHtml("")
                .snippet(snippet)
                .sentAt(entity.getSentAt())
                .direction(entity.getDirection())
                .hasAttachments(content.attachments().stream().anyMatch(att -> !att.isInline()))
                .attachments(content.attachments().stream()
                        .filter(att -> !att.isInline())
                        .map(att -> toAttachmentDto(att, entity.getMessageId()))
                        .collect(Collectors.toList()))
                .build();
    }

    private ContentPayload ensureContentPayload(MailMessageEntity entity) throws MessagingException {
        boolean hasCache = entity.getContentLoadedAt() != null && (StringUtils.hasText(entity.getCachedPlainBody()) || StringUtils.hasText(entity.getCachedHtmlBody()));
        if (hasCache) {
            initializeAttachments(entity);
            String snippet = StringUtils.hasText(entity.getSnippet()) ? entity.getSnippet() : MailTextExtractor.sanitizeSnippet(entity.getCachedPlainBody(), 500);
            return new ContentPayload(entity.getCachedHtmlBody(), entity.getCachedPlainBody(), snippet, entity.getAttachments(), true);
        }

        boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();

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

            String cleanPlain = MailQuotedStripper.stripQuotedPlain(finalPlain);
            String snippet = StringUtils.hasText(cleanPlain)
                    ? MailTextExtractor.sanitizeSnippet(cleanPlain, 500)
                    : MailTextExtractor.sanitizeSnippet(finalPlain, 500);

            if (readOnly) {
                List<MailAttachmentMetaEntity> transientAttachments = collectTransientAttachments(message, "", entity);
                return new ContentPayload(sanitizedHtml, finalPlain, snippet, transientAttachments, false);
            }

            persistContent(entity, sanitizedHtml, finalPlain, cleanPlain, message);
            initializeAttachments(entity);
            return new ContentPayload(sanitizedHtml, finalPlain, entity.getSnippet(), entity.getAttachments(), false);
        } catch (MessagingException e) {
            if (StringUtils.hasText(entity.getSnippet())) {
                return new ContentPayload("", entity.getSnippet(), entity.getSnippet(), entity.getAttachments(), true);
            }
            throw e;
        } catch (IOException e) {
            throw new MessagingException("Failed to load message content", e);
        }
    }

    private void persistContent(MailMessageEntity entity, String html, String plain, String cleanPlain, Message message) throws MessagingException, IOException {
        String sanitizedPlain = StringUtils.hasText(plain) ? plain : "";
        entity.setCachedHtmlBody(html);
        entity.setCachedPlainBody(sanitizedPlain);

        String snippetSource = StringUtils.hasText(cleanPlain) ? cleanPlain : sanitizedPlain;
        entity.setSnippet(MailTextExtractor.sanitizeSnippet(snippetSource, 500));

        List<MailAttachmentMetaEntity> attachments = new ArrayList<>();
        collectAttachments(message, "", attachments);
        attachments.forEach(a -> a.setMessage(entity));
        initializeAttachments(entity);
        entity.getAttachments().clear();
        entity.getAttachments().addAll(attachments);
        entity.setHasAttachments(attachments.stream().anyMatch(att -> !att.isInline()));
        entity.setContentLoadedAt(Instant.now());
        mailMessageRepository.save(entity);

        ChatEntity chat = entity.getChat();
        if (chat != null && (chat.getLastMessageAt() == null
                || entity.getSentAt() == null
                || !chat.getLastMessageAt().isAfter(entity.getSentAt()))) {
            chat.setLastSnippet(entity.getSnippet());
            chat.setHasAttachments(chat.isHasAttachments() || entity.isHasAttachments());
            chatRepository.save(chat);
        }
    }

    private void initializeAttachments(MailMessageEntity entity) {
        if (entity.getAttachments() != null) {
            entity.getAttachments().size();
        }
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

        String contentId = MailpartExtractor.extractContentId(part);
        boolean inline = Part.INLINE.equalsIgnoreCase(part.getDisposition()) || (StringUtils.hasText(contentId) && part.isMimeType("image/*"));
        boolean downloadable = Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || StringUtils.hasText(part.getFileName());

        if (inline || downloadable) {
            MailAttachmentMetaEntity meta = MailAttachmentMetaEntity.builder()
                    .partId(partId.isEmpty() ? "part" : partId)
                    .filename(part.getFileName())
                    .contentType(part.getContentType())
                    .sizeBytes(part.getSize() >= 0 ? (long) part.getSize() : null)
                    .contentId(contentId)
                    .inline(inline)
                    .build();
            attachments.add(meta);
        }
    }

    private static class Extracted {
        final String plain;
        final String html;
        Extracted(String plain, String html) { this.plain = plain; this.html = html; }
    }

    private Extracted extractTextFallback(Part part) throws MessagingException {
        try {
            if (part.isMimeType("text/plain")) {
                String text = safeText(part);
                return new Extracted(StringUtils.hasText(text) ? text : null, null);
            }
            if (part.isMimeType("text/html")) {
                String html = safeText(part);
                return new Extracted(null, StringUtils.hasText(html) ? html : null);
            }

            if (part.isMimeType("message/rfc822")) {
                Object c = part.getContent();
                if (c instanceof Part p) return extractTextFallback(p);
                return new Extracted(null, null);
            }

            if (part.isMimeType("multipart/*")) {
                Multipart mp = (Multipart) part.getContent();
                String plain = null;
                String html = null;

                boolean isAlternative = part.isMimeType("multipart/alternative");
                boolean isRelated = part.isMimeType("multipart/related");

                for (int i = 0; i < mp.getCount(); i++) {
                    BodyPart bp = mp.getBodyPart(i);

                    if (shouldSkipBodyPart(bp)) continue;

                    Extracted ex = extractTextFallback(bp);

                    if (StringUtils.hasText(ex.html) && !StringUtils.hasText(html)) {
                        html = ex.html;
                        if (isAlternative && StringUtils.hasText(plain)) break;
                    }
                    if (StringUtils.hasText(ex.plain) && !StringUtils.hasText(plain)) {
                        plain = ex.plain;
                        if (isAlternative && StringUtils.hasText(html)) break;
                    }

                    if (isRelated && StringUtils.hasText(html)) {
                        // intentionally continue to allow plain extraction if available
                    }
                }

                return new Extracted(plain, html);
            }

            return new Extracted(null, null);

        } catch (Exception ex) {
            throw new MessagingException("Failed to extract body fallback", ex);
        }
    }

    private boolean shouldSkipBodyPart(BodyPart bp) throws MessagingException {
        String disp = bp.getDisposition();
        String fileName = bp.getFileName();
        String ct = bp.getContentType() != null ? bp.getContentType().toLowerCase() : "";

        if (disp != null && disp.equalsIgnoreCase(Part.ATTACHMENT)) return true;
        if (disp != null && disp.equalsIgnoreCase(Part.INLINE) && StringUtils.hasText(fileName)) return true;
        if (disp != null && disp.equalsIgnoreCase(Part.INLINE) && ct.startsWith("image/")) return true;
        if (ct.startsWith("application/")) return true;
        if (StringUtils.hasText(fileName) && !bp.isMimeType("text/*")) return true;

        return false;
    }

    private String safeText(Part part) throws Exception {
        Object c = part.getContent();
        if (c == null) return null;

        if (c instanceof String s) return s;

        if (c instanceof java.io.InputStream is) {
            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }

        return c.toString();
    }

    private AttachmentDto toAttachmentDto(MailAttachmentMetaEntity attachment, String messageId) {
        return AttachmentDto.builder()
                .id(attachment.getId())
                .messageId(messageId)
                .attachmentId(attachment.getPartId())
                .filename(attachment.getFilename())
                .sizeBytes(attachment.getSizeBytes())
                .mimeType(attachment.getContentType())
                .build();
    }

    private String rewriteInlineCid(String html, Long messageId) {
        if (!StringUtils.hasText(html) || messageId == null) {
            return html != null ? html : "";
        }
        Document document = Jsoup.parse(html);
        document.select("img[src^=cid:], img[src^=CID:]").forEach(img -> {
            String src = img.attr("src");
            String cid = src.length() > 4 ? src.substring(4) : "";
            String url = "/api/mail/messages/" + messageId + "/inline/" + UriUtils.encodePath(cid, StandardCharsets.UTF_8);
            img.attr("src", url);
        });
        return document.body().html();
    }

    private String toSimpleHtml(String text) {
        return HtmlUtils.htmlEscape(text).replace("\n", "<br/>");
    }

    private String normalizeContentId(String contentId) {
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

    private List<MailAttachmentMetaEntity> collectTransientAttachments(Part part, String partId, MailMessageEntity entity) throws MessagingException, IOException {
        List<MailAttachmentMetaEntity> attachments = new ArrayList<>();
        collectAttachments(part, partId, attachments);
        attachments.forEach(a -> {
            a.setMessage(entity);
            a.setId(null);
        });
        return attachments;
    }

    private record ContentPayload(String html, String plain, String snippet, List<MailAttachmentMetaEntity> attachments, boolean cached) {
    }
}
