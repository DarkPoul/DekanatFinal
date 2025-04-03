package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.DisciplineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisciplineRepository extends JpaRepository<DisciplineEntity, Long> {
    DisciplineEntity findByTitle(String title);
}
