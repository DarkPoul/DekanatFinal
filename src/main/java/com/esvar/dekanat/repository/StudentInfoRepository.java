package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.StudentInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentInfoRepository extends JpaRepository<StudentInfoEntity, Long> {
    StudentInfoEntity findByStudentId(Long studentId);
}
