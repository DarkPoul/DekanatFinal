package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.EduProgramEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EduProgRepository extends JpaRepository<EduProgramEntity, Long> {

}
