package com.esvar.dekanat.mail.v2.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "mail_folder_state")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folder_name", nullable = false, unique = true)
    private String folderName;

    @Column(name = "uid_validity")
    private Long uidValidity;

    @Column(name = "last_seen_uid")
    private Long lastSeenUid;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
