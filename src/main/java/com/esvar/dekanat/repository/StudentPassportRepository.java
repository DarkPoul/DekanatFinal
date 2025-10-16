package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.StudentPassportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentPassportRepository extends JpaRepository<StudentPassportEntity, Long> {
    List<StudentPassportEntity> findAllByStudent_Id(Long studentId);
}
