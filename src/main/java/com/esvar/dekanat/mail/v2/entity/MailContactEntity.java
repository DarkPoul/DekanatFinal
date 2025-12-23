package com.esvar.dekanat.mail.v2.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "mail_contact", indexes = {
        @Index(name = "idx_mail_contact_email", columnList = "email"),
        @Index(name = "idx_mail_contact_display_name", columnList = "display_name"),
        @Index(name = "idx_mail_contact_org_unit", columnList = "org_unit_text")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailContactEntity {

    public enum ContactType {
        INTERNAL,
        EXTERNAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private ContactType type;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "org_unit_text")
    private String orgUnitText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
