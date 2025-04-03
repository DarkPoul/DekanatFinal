package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {
    DepartmentEntity findByAbbreviation(String abbreviation);

    DepartmentEntity findByTitle(String department);
}
