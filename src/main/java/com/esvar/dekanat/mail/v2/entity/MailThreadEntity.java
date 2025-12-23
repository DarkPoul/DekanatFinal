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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "mail_thread", indexes = {
        @Index(name = "idx_mail_thread_last_incoming_status", columnList = "last_incoming_at, status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailThreadEntity {

    public enum ThreadStatus {
        NEW,
        IN_PROGRESS,
        CLOSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false, unique = true)
    private MailContactEntity contact;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ThreadStatus status;

    @Column(name = "last_incoming_at")
    private Instant lastIncomingAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "last_viewed_at")
    private Instant lastViewedAt;

    @Column(name = "unread_incoming_count", nullable = false)
    private int unreadIncomingCount;
}
