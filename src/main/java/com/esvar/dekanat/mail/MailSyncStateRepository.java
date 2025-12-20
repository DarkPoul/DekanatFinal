package com.esvar.dekanat.mail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MailSyncStateRepository extends JpaRepository<MailSyncStateEntity, String> {
}
