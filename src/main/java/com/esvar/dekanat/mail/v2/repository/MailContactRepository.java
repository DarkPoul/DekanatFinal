package com.esvar.dekanat.mail.v2.repository;

import com.esvar.dekanat.mail.v2.entity.MailContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MailContactRepository extends JpaRepository<MailContactEntity, Long> {

    Optional<MailContactEntity> findByEmailIgnoreCase(String email);

    @Query("select c from MailContactEntity c where lower(c.email) = lower(:email)")
    Optional<MailContactEntity> findNormalized(@Param("email") String email);
}
