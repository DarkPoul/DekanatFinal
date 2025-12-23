package com.esvar.dekanat.mail.v2.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "mail_message_v2", indexes = {
        @Index(name = "idx_mail_message_thread_sent", columnList = "thread_id, sent_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailMessageEntity {

    public enum Direction {
        IN,
        OUT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private MailThreadEntity thread;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 8)
    private Direction direction;

    @Column(name = "from_email", length = 320)
    private String fromEmail;

    @Column(name = "to_email", length = 320)
    private String toEmail;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "body_html", columnDefinition = "LONGTEXT")
    private String bodyHtml;

    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;

    @Column(name = "snippet", length = 200)
    private String snippet;

    @Column(name = "external_id", length = 512, unique = true)
    private String externalId;

    @Column(name = "has_attachments", nullable = false)
    private boolean hasAttachments;
}
