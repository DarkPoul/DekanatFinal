package com.esvar.dekanat.mail.v2.service;

import com.esvar.dekanat.mail.v2.repository.FolderStateRepository;
import com.esvar.dekanat.mail.v2.repository.MailAttachmentRepository;
import com.esvar.dekanat.mail.v2.repository.MailContactRepository;
import com.esvar.dekanat.mail.v2.repository.MailMessageRepository;
import com.esvar.dekanat.mail.v2.repository.MailThreadRepository;
import com.esvar.dekanat.mail.v2.entity.FolderStateEntity;
import com.esvar.dekanat.mail.v2.entity.MailAttachmentEntity;
import com.esvar.dekanat.mail.v2.entity.MailContactEntity;
import com.esvar.dekanat.mail.v2.entity.MailMessageEntity;
import com.esvar.dekanat.mail.v2.entity.MailThreadEntity;
import com.esvar.dekanat.service.DepartmentService;
import com.esvar.dekanat.service.FacultyService;
import com.esvar.dekanat.user.UserModel;
import com.esvar.dekanat.user.UserRepository;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeUtility;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImapMailIngestService implements MailIngestService {

    private final FolderStateRepository folderStateRepository;
    private final MailContactRepository contactRepository;
    private final MailThreadRepository threadRepository;
    private final MailMessageRepository messageRepository;
    private final MailAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final FacultyService facultyService;
    private final DepartmentService departmentService;

    @Value("${mail.sync.enabled:true}")
    private boolean syncEnabled;

    @Value("${mail.imap.host}")
    private String host;

    @Value("${mail.imap.port:993}")
    private int port;

    @Value("${mail.imap.username}")
    private String username;

    @Value("${mail.imap.password}")
    private String password;

    @Value("${mail.imap.inbox-folder:INBOX}")
    private String inboxFolder;

    @Value("${mail.imap.sent-folder:}")
    private String sentFolder;

    @Value("${mail.imap.use-ssl:true}")
    private boolean useSsl;

    @Value("${mail.default-from:}")
    private String defaultFrom;

    @SneakyThrows
    @Override
    public void syncInbox() {
        if (!syncEnabled) {
            return;
        }
        Properties props = new Properties();
        props.put("mail.store.protocol", useSsl ? "imaps" : "imap");
        props.put("mail.imap.ssl.enable", String.valueOf(useSsl));
        props.put("mail.imap.port", String.valueOf(port));
        Session session = Session.getInstance(props);
        try (Store store = session.getStore()) {
            store.connect(host, port, username, password);
            syncFolder(store, inboxFolder);
            if (StringUtils.hasText(sentFolder) && !inboxFolder.equalsIgnoreCase(sentFolder)) {
                syncFolder(store, sentFolder);
            }
        } catch (Exception e) {
            log.error("IMAP sync failed: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelayString = "${mail.sync.interval-ms:60000}")
    public void scheduledSync() {
        syncInbox();
    }

    private void syncFolder(Store store, String folderName) throws MessagingException {
        syncFolder(store, folderName, 0);
    }

    private void syncFolder(Store store, String folderName, int attempt) throws MessagingException {
        try {
            syncFolderInternal(store, folderName);
        } catch (FolderClosedException e) {
            if (attempt < 1) {
                log.warn("Folder {} connection lost, retrying sync", folderName, e);
                syncFolder(store, folderName, attempt + 1);
            } else {
                log.error("Folder {} connection lost after retry, aborting sync", folderName, e);
                throw e;
            }
        }
    }

    private void syncFolderInternal(Store store, String folderName) throws MessagingException {
        Folder folder = store.getFolder(folderName);
        if (!(folder instanceof IMAPFolder imapFolder)) {
            log.warn("Folder {} is not IMAP compatible", folderName);
            return;
        }
        try {
            imapFolder.open(Folder.READ_ONLY);
            FolderStateEntity state = folderStateRepository.findByFolderName(folderName)
                    .orElseGet(() -> FolderStateEntity.builder()
                            .folderName(folderName)
                            .build());
            long uidValidity = imapFolder.getUIDValidity();
            if (state.getUidValidity() != null && !state.getUidValidity().equals(uidValidity)) {
                log.info("UIDVALIDITY changed for {}, resetting sync cursor", folderName);
                state.setLastSeenUid(null);
            }
            state.setUidValidity(uidValidity);

            long startUid = state.getLastSeenUid() != null ? state.getLastSeenUid() + 1 : 1;
            Message[] messages = imapFolder.getMessagesByUID(startUid, UIDFolder.LASTUID);
            FetchProfile fp = new FetchProfile();
            fp.add(UIDFolder.FetchProfileItem.UID);
            imapFolder.fetch(messages, fp);

            Arrays.sort(messages, (m1, m2) -> {
                try {
                    return Long.compare(imapFolder.getUID(m1), imapFolder.getUID(m2));
                } catch (MessagingException e) {
                    throw new RuntimeException("Помилка при отриманні UID під час сортування", e);
                }
            });

            long maxUid = state.getLastSeenUid() != null ? state.getLastSeenUid() : 0;
            for (Message message : messages) {
                long uid = imapFolder.getUID(message);
                if (uid <= 0) {
                    continue;
                }
                try {
                    ingestMessage(message, uid, folderName);
                    maxUid = Math.max(maxUid, uid);
                } catch (Exception e) {
                    log.warn("Failed to ingest message UID {} from folder {}: {}", uid, folderName, e.getMessage(), e);
                }
            }
            state.setLastSeenUid(maxUid);
            state.setUpdatedAt(Instant.now());
            folderStateRepository.save(state);
        } finally {
            if (imapFolder.isOpen()) {
                imapFolder.close(false);
            }
        }
    }

    private void ingestMessage(Message message, long uid, String folderName) throws MessagingException, IOException {
        String messageId = Arrays.toString(message.getHeader("Message-ID"));
        String externalId = StringUtils.hasText(messageId) ? messageId : folderName + ":" + uid;
        if (messageRepository.findByExternalId(externalId).isPresent()) {
            return;
        }

        EmailAddress from = extractAddress(message.getFrom());
        EmailAddress to = extractAddress(message.getRecipients(Message.RecipientType.TO));
        MailMessageEntity.Direction direction = resolveDirection(from.email());
        Instant sentAt = message.getSentDate() != null ? message.getSentDate().toInstant() : Instant.now();
        MessageContent content = extractContent(message);
        MailContactEntity contact = resolveContact(direction, from, to);
        MailThreadEntity thread = resolveThread(contact);

        MailMessageEntity saved = messageRepository.save(MailMessageEntity.builder()
                .thread(thread)
                .direction(direction)
                .fromEmail(from.email())
                .toEmail(to.email())
                .subject(message.getSubject())
                .sentAt(sentAt)
                .bodyHtml(content.bodyHtml())
                .bodyText(content.bodyText())
                .snippet(buildSnippet(content))
                .externalId(externalId)
                .hasAttachments(!content.attachments().isEmpty())
                .build());

        persistAttachments(saved, content.attachments());
        updateThreadMetadata(thread, direction, sentAt);
    }

    private MailThreadEntity resolveThread(MailContactEntity contact) {
        return threadRepository.findByContactId(contact.getId())
                .orElseGet(() -> threadRepository.save(MailThreadEntity.builder()
                        .contact(contact)
                        .status(MailThreadEntity.ThreadStatus.NEW)
                        .unreadIncomingCount(0)
                        .build()));
    }

    private void updateThreadMetadata(MailThreadEntity thread, MailMessageEntity.Direction direction, Instant sentAt) {
        boolean changed = false;
        if (direction == MailMessageEntity.Direction.IN) {
            if (thread.getLastIncomingAt() == null || sentAt.isAfter(thread.getLastIncomingAt())) {
                thread.setLastIncomingAt(sentAt);
            }
            thread.setUnreadIncomingCount(thread.getUnreadIncomingCount() + 1);
            changed = true;
        }
        if (thread.getLastActivityAt() == null || sentAt.isAfter(thread.getLastActivityAt())) {
            thread.setLastActivityAt(sentAt);
            changed = true;
        }
        if (changed) {
            threadRepository.save(thread);
        }
    }

    private MailContactEntity resolveContact(MailMessageEntity.Direction direction, EmailAddress from, EmailAddress to) throws MessagingException {
        EmailAddress peer = direction == MailMessageEntity.Direction.IN ? from : preferredRecipient(to, from);
        String email = peer.email();
        if (!StringUtils.hasText(email)) {
            email = direction == MailMessageEntity.Direction.IN ? to.email() : from.email();
        }
        if (!StringUtils.hasText(email)) {
            throw new MessagingException("Cannot resolve contact email for message");
        }
        String finalEmail = email;
        MailContactEntity contact = contactRepository.findNormalized(email)
                .orElseGet(() -> contactRepository.save(MailContactEntity.builder()
                        .type(MailContactEntity.ContactType.EXTERNAL)
                        .email(finalEmail)
                        .displayName(peer.displayName())
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build()));
        contact = enrichContact(contact, peer);
        if (!StringUtils.hasText(contact.getDisplayName()) && StringUtils.hasText(peer.displayName())) {
            contact.setDisplayName(peer.displayName());
            contact.setUpdatedAt(Instant.now());
            contact = contactRepository.save(contact);
        }
        return contact;
    }

    private MailContactEntity enrichContact(MailContactEntity contact, EmailAddress peer) {
        boolean changed = false;
        Optional<UserModel> userOpt = userRepository.findByEmail(contact.getEmail());
        if (userOpt.isPresent()) {
            UserModel user = userOpt.get();
            if (contact.getType() != MailContactEntity.ContactType.INTERNAL) {
                contact.setType(MailContactEntity.ContactType.INTERNAL);
                changed = true;
            }
            if (contact.getUserId() == null || !contact.getUserId().equals(user.getId())) {
                contact.setUserId(user.getId());
                changed = true;
            }
            String fullName = composeFullName(user);
            if (StringUtils.hasText(fullName) && !fullName.equals(contact.getDisplayName())) {
                contact.setDisplayName(fullName);
                changed = true;
            }
            String orgUnit = resolveOrgUnit(user);
            if (StringUtils.hasText(orgUnit) && !orgUnit.equals(contact.getOrgUnitText())) {
                contact.setOrgUnitText(orgUnit);
                changed = true;
            }
        } else if (!StringUtils.hasText(contact.getDisplayName()) && StringUtils.hasText(peer.displayName())) {
            contact.setDisplayName(peer.displayName());
            changed = true;
        }

        if (changed) {
            contact.setUpdatedAt(Instant.now());
            return contactRepository.save(contact);
        }
        return contact;
    }

    private String composeFullName(UserModel user) {
        return List.of(user.getLastname(), user.getFirstname(), user.getPatronymic())
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.joining(" "));
    }

    private String resolveOrgUnit(UserModel user) {
        String role = user.getRole();
        String roleType = user.getRoleType();
        if (!StringUtils.hasText(role) || !StringUtils.hasText(roleType)) {
            return null;
        }
        try {
            Long roleTypeId = Long.valueOf(roleType);
            if (role.startsWith("ROLE_DEPARTMENT")) {
                return departmentService.getDepartmentById(roleTypeId);
            }
            if (role.startsWith("ROLE_DEKANAT")) {
                return facultyService.getFacultyTitleById(roleTypeId);
            }
        } catch (NumberFormatException e) {
            log.warn("Не вдалося обробити roleType {} для користувача {}", roleType, user.getEmail());
        }
        return null;
    }

    private EmailAddress preferredRecipient(EmailAddress primary, EmailAddress fallback) {
        if (primary != null && StringUtils.hasText(primary.email())) {
            return primary;
        }
        return fallback;
    }

    private MessageContent extractContent(Part part) throws MessagingException, IOException {
        MessageContentBuilder builder = new MessageContentBuilder();
        parsePart(part, builder);
        return builder.build();
    }

    private void parsePart(Part part, MessageContentBuilder builder) throws MessagingException, IOException {
        if (part.isMimeType("multipart/alternative")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/html")) {
                    builder.setBodyHtml(readAsString(bp));
                } else if (bp.isMimeType("text/plain") && !builder.hasText()) {
                    builder.setBodyText(readAsString(bp));
                }
            }
        } else if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                parsePart(mp.getBodyPart(i), builder);
            }
        } else if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || Part.INLINE.equalsIgnoreCase(part.getDisposition()) || StringUtils.hasText(part.getFileName())) {
            builder.addAttachment(buildAttachment(part));
        } else if (part.isMimeType("text/html")) {
            builder.setBodyHtml(readAsString(part));
        } else if (part.isMimeType("text/plain")) {
            builder.setBodyText(readAsString(part));
        }
    }

    private AttachmentData buildAttachment(Part part) throws MessagingException, IOException {
        String filename = MimeUtility.decodeText(part.getFileName() != null ? part.getFileName() : "attachment");
        String contentType = part.getContentType();
        String[] headers = part.getHeader("Content-ID");
        String contentId = (headers != null && headers.length > 0)
                ? headers[0].replaceAll("[<>]", "")
                : null;
        boolean inline = Part.INLINE.equalsIgnoreCase(part.getDisposition()) || StringUtils.hasText(contentId);
        byte[] contentBytes = readBytes(part);
        return new AttachmentData(
                filename,
                contentType,
                (long) contentBytes.length,
                inline,
                contentId,
                Base64.getEncoder().encodeToString(contentBytes)
        );
    }

    private void persistAttachments(MailMessageEntity message, List<AttachmentData> attachments) {
        if (attachments.isEmpty()) {
            return;
        }
        List<MailAttachmentEntity> entities = new ArrayList<>();
        for (AttachmentData attachment : attachments) {
            entities.add(MailAttachmentEntity.builder()
                    .message(message)
                    .filename(attachment.filename())
                    .contentType(attachment.contentType())
                    .size(attachment.size())
                    .inline(attachment.inline())
                    .contentId(attachment.contentId())
                    .storageType(MailAttachmentEntity.StorageType.DB)
                    .storageKey(attachment.storageKey())
                    .createdAt(Instant.now())
                    .build());
        }
        attachmentRepository.saveAll(entities);
    }

    private MailMessageEntity.Direction resolveDirection(String fromEmail) {
        if (StringUtils.hasText(fromEmail)) {
            if (StringUtils.hasText(defaultFrom) && defaultFrom.equalsIgnoreCase(fromEmail)) {
                return MailMessageEntity.Direction.OUT;
            }
            if (StringUtils.hasText(username) && username.equalsIgnoreCase(fromEmail)) {
                return MailMessageEntity.Direction.OUT;
            }
        }
        return MailMessageEntity.Direction.IN;
    }

    private String buildSnippet(MessageContent content) {
        String source = StringUtils.hasText(content.bodyText()) ? content.bodyText() : content.bodyHtml();
        if (!StringUtils.hasText(source)) {
            return null;
        }
        String cleaned = source.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.length() > 200) {
            return cleaned.substring(0, 200);
        }
        return cleaned;
    }

    private EmailAddress extractAddress(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return new EmailAddress(null, null);
        }
        Address address = addresses[0];
        if (address instanceof InternetAddress internetAddress) {
            return new EmailAddress(internetAddress.getAddress(), internetAddress.getPersonal());
        }
        return new EmailAddress(address.toString(), null);
    }

    private String readAsString(Part part) throws IOException, MessagingException {
        Object content = part.getContent();
        if (content instanceof String) {
            return (String) content;
        }
        return new String(readBytes(part), StandardCharsets.UTF_8);
    }

    private byte[] readBytes(Part part) throws IOException, MessagingException {
        try (InputStream inputStream = part.getInputStream()) {
            return inputStream.readAllBytes();
        }
    }

    private record EmailAddress(String email, String displayName) {
    }

    private record AttachmentData(String filename,
                                  String contentType,
                                  Long size,
                                  boolean inline,
                                  String contentId,
                                  String storageKey) {
    }

    private static class MessageContent {
        private final String bodyText;
        private final String bodyHtml;
        private final List<AttachmentData> attachments;

        MessageContent(String bodyText, String bodyHtml, List<AttachmentData> attachments) {
            this.bodyText = bodyText;
            this.bodyHtml = bodyHtml;
            this.attachments = attachments;
        }

        public String bodyText() {
            return bodyText;
        }

        public String bodyHtml() {
            return bodyHtml;
        }

        public List<AttachmentData> attachments() {
            return attachments;
        }
    }

    private static class MessageContentBuilder {
        private String bodyText;
        private String bodyHtml;
        private final List<AttachmentData> attachments = new ArrayList<>();

        public void setBodyText(String bodyText) {
            if (!StringUtils.hasText(this.bodyText)) {
                this.bodyText = bodyText;
            }
        }

        public void setBodyHtml(String bodyHtml) {
            if (!StringUtils.hasText(this.bodyHtml)) {
                this.bodyHtml = bodyHtml;
            }
        }

        public void addAttachment(AttachmentData attachment) {
            attachments.add(attachment);
        }

        public boolean hasText() {
            return StringUtils.hasText(bodyText) || StringUtils.hasText(bodyHtml);
        }

        public MessageContent build() {
            return new MessageContent(bodyText, bodyHtml, attachments);
        }
    }
}
