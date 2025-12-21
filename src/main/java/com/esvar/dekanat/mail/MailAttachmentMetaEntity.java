package com.esvar.dekanat.mail;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mail_attachment_meta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailAttachmentMetaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private MailMessageEntity message;

    @Column(name = "part_id", nullable = false, length = 128)
    private String partId;

    @Column(name = "filename", length = 500)
    private String filename;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "content_id", length = 255)
    private String contentId;

    @Column(name = "inline_attachment", nullable = false)
    private boolean inline;
}
