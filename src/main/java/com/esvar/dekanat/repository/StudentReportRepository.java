package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentReportRepository extends JpaRepository<ReportEntity, Long> {

    List<ReportEntity> findByStudentId(Long studentId);
}
