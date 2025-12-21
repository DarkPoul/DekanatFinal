package com.esvar.dekanat.mail;


import jakarta.mail.Address;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class MailSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailSyncService.class);

    private final MailImapClient mailImapClient;
    private final MailSyncProperties properties;
    private final ChatRepository chatRepository;
    private final MailMessageRepository mailMessageRepository;
    private final MailSyncStateRepository syncStateRepository;
    private final ChatProfileResolver profileResolver;
    private final org.springframework.boot.autoconfigure.mail.MailProperties mailProperties;

    public MailSyncService(MailImapClient mailImapClient,
                           MailSyncProperties properties,
                           ChatRepository chatRepository,
                           MailMessageRepository mailMessageRepository,
                           MailSyncStateRepository syncStateRepository,
                           ChatProfileResolver profileResolver,
                           org.springframework.boot.autoconfigure.mail.MailProperties mailProperties) {
        this.mailImapClient = mailImapClient;
        this.properties = properties;
        this.chatRepository = chatRepository;
        this.mailMessageRepository = mailMessageRepository;
        this.syncStateRepository = syncStateRepository;
        this.profileResolver = profileResolver;
        this.mailProperties = mailProperties;
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

        String contactEmail = resolveContactEmail(message, direction);
        if (!StringUtils.hasText(contactEmail)) {
            LOGGER.debug("Skipping message {} without contact email", messageId);
            return;
        }

        String normalizedSubject = MailThreadUtils.normalizeSubject(message.getSubject());
        String threadKey = MailThreadUtils.buildThreadKey(contactEmail, message.getSubject());
        String title = MailThreadUtils.stripPrefixes(message.getSubject());

        ChatEntity chat = chatRepository.findByContactEmail(contactEmail)
                .or(() -> chatRepository.findByPeerEmail(contactEmail))
                .orElseGet(() -> createChat(contactEmail, title, normalizedSubject));

        String cleanPlain = MailQuotedStripper.stripQuotedPlain(MailpartExtractor.extractPlainText(message));
        String snippet = MailTextExtractor.sanitizeSnippet(StringUtils.hasText(cleanPlain) ? cleanPlain : title, 500);

        MailMessageEntity entity = MailMessageEntity.builder()
                .messageId(messageId)
                .chat(chat)
                .threadKey(threadKey)
                .normalizedSubject(normalizedSubject)
                .peerEmail(contactEmail)
                .contactEmail(contactEmail)
                .folder(folder.getFullName())
                .uid(folder.getUID(message))
                .sentAt(resolveSentAt(message))
                .fromEmail(extractAddress(message.getFrom()))
                .toEmail(extractAddress(message.getRecipients(Message.RecipientType.TO)))
                .subject(message.getSubject())
                .hasAttachments(hasAttachments(message))
                .snippet(snippet)
                .direction(direction)
                .build();

        mailMessageRepository.save(entity);

        chat.setThreadKey(threadKey);
        chat.setTitle(title);
        chat.setNormalizedSubject(normalizedSubject);
        chat.setPeerEmail(contactEmail);
        chat.setContactEmail(contactEmail);
        chat.setLastMessageAt(entity.getSentAt());
        chat.setLastSnippet(entity.getSnippet());
        chat.setHasAttachments(chat.isHasAttachments() || entity.isHasAttachments());
        if (direction == MessageDirection.IN) {
            chat.setHasUnprocessed(true);
            chat.setUnreadCount(chat.getUnreadCount() + 1);
        }
        chatRepository.save(chat);
    }

    private boolean hasAttachments(Part part) throws MessagingException, IOException {
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                Part bodyPart = multipart.getBodyPart(i);
                if (isAttachment(bodyPart) || hasAttachments(bodyPart)) {
                    return true;
                }
            }
        }
        return isAttachment(part);
    }

    private boolean isAttachment(Part part) throws MessagingException {
        return Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || StringUtils.hasText(part.getFileName());
    }

    private String resolveContactEmail(Message message, MessageDirection direction) throws MessagingException {
        if (direction == MessageDirection.IN) {
            return normalizeEmail(extractAddress(message.getFrom()));
        }
        List<String> recipients = extractAddresses(message.getRecipients(Message.RecipientType.TO));
        if (recipients.isEmpty()) {
            return null;
        }
        String external = findExternalRecipient(recipients);
        return normalizeEmail(StringUtils.hasText(external) ? external : recipients.get(0));
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

    private List<String> extractAddresses(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Address address : addresses) {
            if (address instanceof InternetAddress internetAddress) {
                result.add(internetAddress.getAddress());
            } else {
                result.add(address.toString());
            }
        }
        return result;
    }

    private String findExternalRecipient(List<String> recipients) {
        Set<String> localAddresses = localAddresses();
        for (String recipient : recipients) {
            if (recipient == null) {
                continue;
            }
            String normalized = recipient.trim().toLowerCase();
            if (!localAddresses.contains(normalized)) {
                return recipient;
            }
        }
        return null;
    }

    private Set<String> localAddresses() {
        Set<String> addresses = new HashSet<>();
        if (StringUtils.hasText(properties.getImap().getUsername())) {
            addresses.add(properties.getImap().getUsername().trim().toLowerCase());
        }
        if (mailProperties != null && StringUtils.hasText(mailProperties.getUsername())) {
            addresses.add(mailProperties.getUsername().trim().toLowerCase());
        }
        return addresses;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private ChatEntity createChat(String contactEmail, String title, String normalizedSubject) {
        ChatProfileResolver.ResolvedProfile profile = profileResolver.resolve(contactEmail);
        return chatRepository.save(ChatEntity.builder()
                .threadKey(contactEmail)
                .peerEmail(contactEmail)
                .contactEmail(contactEmail)
                .title(title)
                .normalizedSubject(normalizedSubject)
                .displayName(profile.displayName())
                .orgUnit(profile.orgUnit())
                .status(ChatStatus.NEW)
                .hasUnprocessed(false)
                .unreadCount(0)
                .hasAttachments(false)
                .lastSnippet(null)
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
}
