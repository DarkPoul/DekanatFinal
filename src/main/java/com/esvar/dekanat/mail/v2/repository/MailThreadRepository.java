package com.esvar.dekanat.mail.v2.repository;

import com.esvar.dekanat.mail.v2.entity.MailThreadEntity;
import com.esvar.dekanat.mail.v2.entity.MailThreadEntity.ThreadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MailThreadRepository extends JpaRepository<MailThreadEntity, Long> {

    @Query("""
            select t from MailThreadEntity t
            join fetch t.contact c
            where (:status is null or t.status = :status)
            and (:name is null or lower(c.displayName) like lower(concat('%', :name, '%'))
                 or lower(c.email) like lower(concat('%', :name, '%')))
            and (:email is null or lower(c.email) like lower(concat('%', :email, '%')))
            and (:org is null or lower(c.orgUnitText) like lower(concat('%', :org, '%')))
            order by t.lastIncomingAt desc
            """)
    Page<MailThreadEntity> search(@Param("name") String name,
                                  @Param("email") String email,
                                  @Param("org") String org,
                                  @Param("status") ThreadStatus status,
                                  Pageable pageable);

    @Query("select t from MailThreadEntity t join fetch t.contact where t.id = :id")
    Optional<MailThreadEntity> findWithContact(@Param("id") Long id);

    Optional<MailThreadEntity> findByContactId(Long contactId);
}
