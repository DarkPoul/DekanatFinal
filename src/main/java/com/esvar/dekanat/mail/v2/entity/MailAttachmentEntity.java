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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "mail_attachment_v2", indexes = {
        @Index(name = "idx_mail_attachment_message_inline", columnList = "message_id, is_inline, content_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailAttachmentEntity {

    public enum StorageType {
        DB,
        FS
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private MailMessageEntity message;

    @Column(name = "filename")
    private String filename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size")
    private Long size;

    @Column(name = "is_inline", nullable = false)
    private boolean inline;

    @Column(name = "content_id")
    private String contentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 8)
    private StorageType storageType;

    @Lob
    @Column(name = "storage_key", columnDefinition = "LONGTEXT")
    private String storageKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
