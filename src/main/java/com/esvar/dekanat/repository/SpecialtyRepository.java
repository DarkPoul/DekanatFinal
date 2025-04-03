package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.SpecialtyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecialtyRepository extends JpaRepository<SpecialtyEntity, Long> {
    SpecialtyEntity findByAbbreviation(String abr);
}
