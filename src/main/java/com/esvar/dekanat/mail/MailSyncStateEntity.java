package com.esvar.dekanat.mail;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mail_sync_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MailSyncStateEntity {

    @Id
    @Column(name = "folder", length = 64)
    private String folder;

    @Column(name = "last_uid")
    private Long lastUid;
}
