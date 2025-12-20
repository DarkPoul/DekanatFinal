package com.esvar.dekanat.mail;


import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class MailSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailSyncService.class);

    private final MailImapClient mailImapClient;
    private final MailSyncProperties properties;
    private final ChatRepository chatRepository;
    private final MailMessageRepository mailMessageRepository;
    private final MailAttachmentMetaRepository attachmentRepository;
    private final MailSyncStateRepository syncStateRepository;
    private final ChatProfileResolver profileResolver;

    public MailSyncService(MailImapClient mailImapClient,
                           MailSyncProperties properties,
                           ChatRepository chatRepository,
                           MailMessageRepository mailMessageRepository,
                           MailAttachmentMetaRepository attachmentRepository,
                           MailSyncStateRepository syncStateRepository,
                           ChatProfileResolver profileResolver) {
        this.mailImapClient = mailImapClient;
        this.properties = properties;
        this.chatRepository = chatRepository;
        this.mailMessageRepository = mailMessageRepository;
        this.attachmentRepository = attachmentRepository;
        this.syncStateRepository = syncStateRepository;
        this.profileResolver = profileResolver;
    }

    @Scheduled(fixedDelayString = "${mail.sync.interval-ms:60000}")
    @Transactional
    public void scheduledSync() throws MessagingException {
        syncFolder(properties.getImap().getInboxFolder(), MessageDirection.IN);
        syncFolder(properties.getImap().getSentFolder(), MessageDirection.OUT);
    }

    protected void syncFolder(String folderName, MessageDirection direction) throws MessagingException {
        mailImapClient.withFolder(folderName, folder -> {
            try {
                Long lastSyncedUid = syncStateRepository.findById(folderName).map(MailSyncStateEntity::getLastUid).orElse(0L);
                long nextUid = folder.getUIDNext();
                if (nextUid <= 0) {
                    return null;
                }
                long batchSize = properties.getSync().getBatchSize();
                for (long start = lastSyncedUid + 1; start < nextUid; start += batchSize) {
                    long end = Math.min(start + batchSize - 1, nextUid - 1);
                    Message[] messages = folder.getMessagesByUID(start, end);
                    for (Message message : messages) {
                        try {
                            upsertMessage(folder, message, direction);
                            lastSyncedUid = Math.max(lastSyncedUid, folder.getUID(message));
                        } catch (Exception ex) {
                            LOGGER.warn("Failed to sync message {} in {}: {}", safeMessageId(message), folderName, ex.getMessage());
                        }
                    }
                    syncStateRepository.save(new MailSyncStateEntity(folderName, lastSyncedUid));
                }
            } catch (MessagingException e) {
                LOGGER.error("IMAP sync failed for {}: {}", folderName, e.getMessage());
            }
            return null;
        });
    }

    private void upsertMessage(IMAPFolder folder, Message message, MessageDirection direction) throws MessagingException, IOException {
        String messageId = extractMessageId(message);
        if (!StringUtils.hasText(messageId)) {
            LOGGER.debug("Skipping message without Message-ID in folder {}", folder.getFullName());
            return;
        }
        Optional<MailMessageEntity> existing = mailMessageRepository.findByMessageId(messageId);
        if (existing.isPresent()) {
            return;
        }

        String peerEmail = resolvePeerEmail(message, direction);
        if (!StringUtils.hasText(peerEmail)) {
            LOGGER.debug("Skipping message {} without peer email", messageId);
            return;
        }

        ChatEntity chat = chatRepository.findByPeerEmail(peerEmail)
                .orElseGet(() -> createChat(peerEmail));

        ParsedBody parsedBody = parseMessageBody(message);

        MailMessageEntity entity = MailMessageEntity.builder()
                .messageId(messageId)
                .chat(chat)
                .peerEmail(peerEmail)
                .folder(folder.getFullName())
                .uid(folder.getUID(message))
                .sentAt(resolveSentAt(message))
                .fromEmail(extractAddress(message.getFrom()))
                .toEmail(extractAddress(message.getRecipients(Message.RecipientType.TO)))
                .subject(message.getSubject())
                .snippet(MailTextExtractor.sanitizeSnippet(parsedBody.snippet, 500))
                .hasAttachments(!parsedBody.attachments.isEmpty())
                .direction(direction)
                .build();

        parsedBody.attachments.forEach(attachment -> attachment.setMessage(entity));
        entity.setAttachments(parsedBody.attachments);

        mailMessageRepository.save(entity);

        chat.setLastMessageAt(entity.getSentAt());
        if (direction == MessageDirection.IN) {
            chat.setHasUnprocessed(true);
        }
        chatRepository.save(chat);
    }

    private ParsedBody parseMessageBody(Message message) throws MessagingException, IOException {
        List<MailAttachmentMetaEntity> attachments = new ArrayList<>();
        String text = extractText(message, "", attachments);
        return new ParsedBody(text, attachments);
    }

    private String extractText(Part part, String partId, List<MailAttachmentMetaEntity> attachments) throws MessagingException, IOException {
        if (part.isMimeType("text/plain")) {
            return part.getContent().toString();
        }
        if (part.isMimeType("text/html")) {
            return MailTextExtractor.toPlainText(part.getContent().toString());
        }
        if (part.isMimeType("multipart/alternative")) {
            return extractAlternativeText((Multipart) part.getContent(), partId, attachments);
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                Part bodyPart = multipart.getBodyPart(i);
                String childPartId = partId.isEmpty() ? String.valueOf(i + 1) : partId + "." + (i + 1);
                String nestedText = extractText(bodyPart, childPartId, attachments);
                if (StringUtils.hasText(nestedText)) {
                    if (builder.length() > 0) {
                        builder.append("\n\n");
                    }
                    builder.append(nestedText);
                }
            }
            return builder.toString();
        }
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || StringUtils.hasText(part.getFileName())) {
            attachments.add(MailAttachmentMetaEntity.builder()
                    .partId(partId.isEmpty() ? "part" : partId)
                    .filename(part.getFileName())
                    .contentType(part.getContentType())
                    .sizeBytes(part.getSize() >= 0 ? (long) part.getSize() : null)
                    .build());
        }
        return "";
    }

    private String extractAlternativeText(Multipart multipart, String partId, List<MailAttachmentMetaEntity> attachments) throws MessagingException, IOException {
        String plainCandidate = null;
        String htmlCandidate = null;
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String childPartId = partId.isEmpty() ? String.valueOf(i + 1) : partId + "." + (i + 1);
            if (bodyPart.isMimeType("text/plain") && !StringUtils.hasText(plainCandidate)) {
                plainCandidate = extractText(bodyPart, childPartId, attachments);
                continue;
            }
            if (bodyPart.isMimeType("text/html") && !StringUtils.hasText(htmlCandidate)) {
                htmlCandidate = extractText(bodyPart, childPartId, attachments);
                continue;
            }
            String nested = extractText(bodyPart, childPartId, attachments);
            if (!StringUtils.hasText(plainCandidate)) {
                plainCandidate = nested;
            }
        }
        if (StringUtils.hasText(plainCandidate)) {
            return plainCandidate;
        }
        if (StringUtils.hasText(htmlCandidate)) {
            return htmlCandidate;
        }
        return "";
    }

    private String resolvePeerEmail(Message message, MessageDirection direction) throws MessagingException {
        if (direction == MessageDirection.IN) {
            return extractAddress(message.getFrom());
        }
        return extractAddress(message.getRecipients(Message.RecipientType.TO));
    }

    private String extractAddress(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return null;
        }
        Address address = addresses[0];
        if (address instanceof InternetAddress internetAddress) {
            return internetAddress.getAddress();
        }
        return address.toString();
    }

    private ChatEntity createChat(String peerEmail) {
        ChatProfileResolver.ResolvedProfile profile = profileResolver.resolve(peerEmail);
        return chatRepository.save(ChatEntity.builder()
                .peerEmail(peerEmail)
                .displayName(profile.displayName())
                .orgUnit(profile.orgUnit())
                .status(ChatStatus.NEW)
                .hasUnprocessed(false)
                .lastMessageAt(null)
                .build());
    }

    private Instant resolveSentAt(Message message) throws MessagingException {
        Date sentDate = message.getSentDate();
        if (sentDate != null) {
            return sentDate.toInstant();
        }
        Date received = message.getReceivedDate();
        return received != null ? received.toInstant() : Instant.now();
    }

    private String extractMessageId(Message message) {
        try {
            String[] headers = message.getHeader("Message-ID");
            if (headers != null && headers.length > 0) {
                return headers[0];
            }
        } catch (MessagingException e) {
            LOGGER.warn("Cannot read Message-ID: {}", e.getMessage());
        }
        return null;
    }

    private String safeMessageId(Message message) {
        try {
            return extractMessageId(message);
        } catch (Exception ex) {
            return "(unknown)";
        }
    }

    private record ParsedBody(String snippet, List<MailAttachmentMetaEntity> attachments) {
    }
}
