package com.esvar.dekanat.mail;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<ChatEntity, Long> {

    Optional<ChatEntity> findByPeerEmail(String peerEmail);

    Optional<ChatEntity> findByThreadKey(String threadKey);

    @Query("""
            select c from ChatEntity c
            where (:statusesEmpty = true or c.status in :statuses)
              and (lower(c.peerEmail) like lower(concat('%', :query, '%'))
                or lower(coalesce(c.displayName,'')) like lower(concat('%', :query, '%'))
                or lower(coalesce(c.title,'')) like lower(concat('%', :query, '%')))
            """)
    Page<ChatEntity> search(@Param("query") String query,
                            @Param("statuses") Iterable<ChatStatus> statuses,
                            @Param("statusesEmpty") boolean statusesEmpty,
                            Pageable pageable);

}
