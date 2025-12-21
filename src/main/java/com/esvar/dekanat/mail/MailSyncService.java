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
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Instant;
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
    private final MailSyncStateRepository syncStateRepository;
    private final ChatProfileResolver profileResolver;

    public MailSyncService(MailImapClient mailImapClient,
                           MailSyncProperties properties,
                           ChatRepository chatRepository,
                           MailMessageRepository mailMessageRepository,
                           MailSyncStateRepository syncStateRepository,
                           ChatProfileResolver profileResolver) {
        this.mailImapClient = mailImapClient;
        this.properties = properties;
        this.chatRepository = chatRepository;
        this.mailMessageRepository = mailMessageRepository;
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
                .hasAttachments(hasAttachments(message))
                .direction(direction)
                .build();

        mailMessageRepository.save(entity);

        chat.setLastMessageAt(entity.getSentAt());
        if (direction == MessageDirection.IN) {
            chat.setHasUnprocessed(true);
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
}
