package com.esvar.dekanat.mail;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "mail_chat", indexes = {
        @Index(name = "idx_mail_chat_peer_email", columnList = "peer_email", unique = true),
        @Index(name = "idx_mail_chat_last_message_at", columnList = "last_message_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "peer_email", nullable = false, unique = true, length = 320)
    private String peerEmail;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "org_unit", length = 255)
    private String orgUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @ColumnDefault("'NEW'")
    private ChatStatus status = ChatStatus.NEW;

    @Column(name = "has_unprocessed", nullable = false)
    private boolean hasUnprocessed = false;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;
}
