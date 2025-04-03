package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.StudentPassportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentPassportRepository extends JpaRepository<StudentPassportEntity, Long> {
    StudentPassportEntity findByStudentId(Long studentId);
}
