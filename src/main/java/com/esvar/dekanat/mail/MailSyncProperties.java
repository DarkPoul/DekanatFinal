package com.esvar.dekanat.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mail")
public class MailSyncProperties {

    private ImapProperties imap = new ImapProperties();
    private SyncProperties sync = new SyncProperties();

    @Getter
    @Setter
    public static class ImapProperties {
        private String host;
        private Integer port = 993;
        private String username;
        private String password;
        private String inboxFolder = "INBOX";
        private String sentFolder = "Sent";
        private boolean useSsl = true;
    }

    @Getter
    @Setter
    public static class SyncProperties {
        private long intervalMs = 60_000;
        private int batchSize = 100;
    }
}
