package com.esvar.dekanat.mail;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<ChatEntity, Long> {

    Optional<ChatEntity> findByPeerEmail(String peerEmail);

    Page<ChatEntity> findByPeerEmailContainingIgnoreCaseAndDisplayNameContainingIgnoreCaseAndStatusIn(
            String peerEmail, String displayName, Iterable<ChatStatus> statuses, Pageable pageable);

    Page<ChatEntity> findByPeerEmailContainingIgnoreCaseAndDisplayNameContainingIgnoreCase(
            String peerEmail, String displayName, Pageable pageable);
}
