package com.esvar.dekanat.mail;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mail_message", indexes = {
        @Index(name = "idx_mail_message_peer_email_date", columnList = "peer_email, sent_at"),
        @Index(name = "idx_mail_message_folder_uid", columnList = "folder, uid")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true, length = 512)
    private String messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private ChatEntity chat;

    @Column(name = "peer_email", nullable = false, length = 320)
    private String peerEmail;

    @Column(name = "folder", nullable = false, length = 64)
    private String folder;

    @Column(name = "uid", nullable = false)
    private Long uid;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "from_email", length = 320)
    private String fromEmail;

    @Column(name = "to_email", length = 320)
    private String toEmail;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "snippet", length = 1000)
    private String snippet;

    @Lob
    @Column(name = "cached_plain_body")
    private String cachedPlainBody;

    @Lob
    @Column(name = "cached_html_body")
    private String cachedHtmlBody;

    @Column(name = "content_loaded_at")
    private Instant contentLoadedAt;

    @Column(name = "has_attachments", nullable = false)
    private boolean hasAttachments;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 8)
    private MessageDirection direction;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MailAttachmentMetaEntity> attachments = new ArrayList<>();
}
