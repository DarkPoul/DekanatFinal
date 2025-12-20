package com.esvar.dekanat.mail;

import com.esvar.dekanat.mail.dto.AttachmentDto;
import com.esvar.dekanat.mail.dto.ChatListItemDto;
import com.esvar.dekanat.mail.dto.ChatMessageDto;
import com.esvar.dekanat.mail.dto.ChatFilter;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
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

import java.io.InputStream;
import java.util.Collections;
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
    public Page<ChatMessageDto> findMessages(Long chatId, Pageable pageable) {
        Page<MailMessageEntity> page = mailMessageRepository.findByChatId(chatId, pageable);
        return page.map(this::toMessageDto);
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
    public InputStream loadAttachment(String messageId, String attachmentId) {
        MailAttachmentMetaEntity meta = attachmentRepository.findByMessage_MessageIdAndPartId(messageId, attachmentId);
        if (meta == null) {
            throw new IllegalArgumentException("Attachment not found");
        }
        MailMessageEntity message = meta.getMessage();
        Message mimeMessage = mailImapClient.getMessage(message.getFolder(), message.getUid());
        return MailpartExtractor.extractAttachmentStream(mimeMessage, attachmentId);
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
            lastMessage.map(MailMessageEntity::getMessageId).ifPresent(id -> mimeMessage.setHeader("In-Reply-To", id));
            lastMessage.map(MailMessageEntity::getMessageId).ifPresent(id -> mimeMessage.setHeader("References", id));
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

    private ChatMessageDto toMessageDto(MailMessageEntity entity) {
        String bodyText = fetchPlainBody(entity);
        return ChatMessageDto.builder()
                .id(entity.getId())
                .messageId(entity.getMessageId())
                .from(entity.getFromEmail())
                .to(entity.getToEmail())
                .subject(entity.getSubject())
                .bodyText(bodyText)
                .sentAt(entity.getSentAt())
                .direction(entity.getDirection())
                .hasAttachments(entity.isHasAttachments())
                .attachments(entity.getAttachments().stream().map(this::toAttachmentDto).collect(Collectors.toList()))
                .build();
    }

    private String fetchPlainBody(MailMessageEntity entity) {
        Message message = mailImapClient.getMessage(entity.getFolder(), entity.getUid());
        return MailpartExtractor.extractPlainText(message);
    }

    private AttachmentDto toAttachmentDto(MailAttachmentMetaEntity attachment) {
        return AttachmentDto.builder()
                .id(attachment.getId())
                .attachmentId(attachment.getPartId())
                .filename(attachment.getFilename())
                .sizeBytes(attachment.getSizeBytes())
                .build();
    }
}
