package com.esvar.dekanat.mail;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.eclipse.angus.mail.imap.IMAPStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.function.Function;

@Component
public class MailImapClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailImapClient.class);

    private final MailSyncProperties properties;

    /**
     * Щоб не засмічувати логи кожну хвилину — друкуємо список папок лише 1 раз за запуск.
     */
    private volatile boolean foldersLogged = false;

    public MailImapClient(MailSyncProperties properties) {
        this.properties = properties;
    }

    public <T> T withFolder(String folderName, Function<IMAPFolder, T> consumer) throws MessagingException {

        Properties props = new Properties();
        String protocol = properties.getImap().isUseSsl() ? "imaps" : "imap";

        props.put("mail.store.protocol", protocol);

        // Якщо useSsl=true — цього зазвичай достатньо для Gmail:993
        props.put("mail.imap.ssl.enable", String.valueOf(properties.getImap().isUseSsl()));
        // На випадок imaps протоколу теж норм
        props.put("mail.imaps.ssl.enable", String.valueOf(properties.getImap().isUseSsl()));

        Session session = Session.getInstance(props);

        IMAPStore store = null;
        IMAPFolder folder = null;

        try {
            store = (IMAPStore) session.getStore(protocol);

            String host = properties.getImap().getHost();
            int port = properties.getImap().getPort();
            String user = properties.getImap().getUsername();
            String pass = properties.getImap().getPassword();

            store.connect(host, port, user, pass);

            // Друкуємо всі папки лише 1 раз (щоб знайти правильний SENT folder)
            logFoldersOnce(store);

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

    public Message getMessage(String folderName, long uid) throws MessagingException {
        return withFolder(folderName, folder -> {
            try {
                Message message = folder.getMessageByUID(uid);
                if (message == null) {
                    throw new IllegalStateException("Message UID " + uid + " not found in folder " + folderName);
                }
                // Copy message content while folder is open to avoid FolderClosedException later.
                return new MimeMessage((MimeMessage) message);
            } catch (MessagingException e) {
                throw new IllegalStateException("Failed to load message by UID " + uid + " from folder " + folderName, e);
            }
        });
    }

    private void logFoldersOnce(IMAPStore store) {
        if (foldersLogged) {
            return;
        }
        synchronized (this) {
            if (foldersLogged) {
                return;
            }
            try {
                Folder[] folders = store.getDefaultFolder().list("*");
                LOGGER.info("IMAP folders listing (one-time):");
                for (Folder f : folders) {
                    LOGGER.info("IMAP folder: {}", f.getFullName());
                }
                foldersLogged = true;
            } catch (Exception e) {
                // Якщо не вдалось зчитати папки — не валимо синк, просто попереджаємо
                LOGGER.warn("Failed to list IMAP folders: {}", e.getMessage());
                foldersLogged = true; // щоб не повторювати спробу кожен раз
            }
        }
    }
}
