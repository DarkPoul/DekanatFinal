package com.esvar.dekanat.mail.v2.repository;

import com.esvar.dekanat.mail.v2.entity.FolderStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FolderStateRepository extends JpaRepository<FolderStateEntity, Long> {

    Optional<FolderStateEntity> findByFolderName(String folderName);
}
