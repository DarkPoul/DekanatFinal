package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.StudentEducationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentEducationRepository extends JpaRepository<StudentEducationEntity, Long> {
    StudentEducationEntity findByStudentId(Long studentId);
}
