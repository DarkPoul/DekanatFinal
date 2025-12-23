package com.esvar.dekanat.mail.v2.service;

import com.esvar.dekanat.mail.v2.repository.FolderStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImapMailIngestService implements MailIngestService {

    private final FolderStateRepository folderStateRepository;

    @Value("${mail.sync.enabled:true}")
    private boolean syncEnabled;

    @Override
    public void syncInbox() {
        if (!syncEnabled) {
            return;
        }
        log.debug("IMAP sync placeholder executed");
    }

    @Scheduled(fixedDelayString = "${mail.sync.interval-ms:60000}")
    public void scheduledSync() {
        syncInbox();
    }
}
