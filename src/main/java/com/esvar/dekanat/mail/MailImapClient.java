package com.esvar.dekanat.mail;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import jakarta.mail.Message;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.function.Function;

@Component
public class MailImapClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailImapClient.class);

    private final MailSyncProperties properties;

    public MailImapClient(MailSyncProperties properties) {
        this.properties = properties;
    }

    public <T> T withFolder(String folderName, Function<IMAPFolder, T> consumer) {
        Properties props = new Properties();
        props.put("mail.store.protocol", properties.getImap().isUseSsl() ? "imaps" : "imap");
        props.put("mail.imap.ssl.enable", String.valueOf(properties.getImap().isUseSsl()));
        Session session = Session.getInstance(props);
        IMAPStore store = null;
        IMAPFolder folder = null;
        try {
            store = (IMAPStore) session.getStore(properties.getImap().isUseSsl() ? "imaps" : "imap");
            store.connect(properties.getImap().getHost(),
                    properties.getImap().getPort(),
                    properties.getImap().getUsername(),
                    properties.getImap().getPassword());
            folder = (IMAPFolder) store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            return consumer.apply(folder);
        } catch (Exception ex) {
            LOGGER.error("IMAP folder interaction failed for {}: {}", folderName, ex.getMessage());
            throw new IllegalStateException("IMAP interaction failed: " + ex.getMessage(), ex);
        } finally {
            if (folder != null && folder.isOpen()) {
                try {
                    folder.close(false);
                } catch (MessagingException ignored) {
                }
            }
            if (store != null && store.isConnected()) {
                try {
                    store.close();
                } catch (MessagingException ignored) {
                }
            }
        }
    }

    public Message getMessage(String folderName, long uid) {
        return withFolder(folderName, folder -> {
            try {
                return folder.getMessageByUID(uid);
            } catch (MessagingException e) {
                throw new IllegalStateException("Failed to load message by UID " + uid + " from folder " + folderName, e);
            }
        });
    }
}
