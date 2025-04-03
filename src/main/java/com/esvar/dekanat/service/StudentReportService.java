package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.ReportEntity;
import com.esvar.dekanat.repository.StudentReportRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentReportService {
    private final StudentReportRepository studentReportRepository;

    public StudentReportService(StudentReportRepository studentReportRepository) {
        this.studentReportRepository = studentReportRepository;
    }

    public List<ReportEntity> getReportsByStudentId(Long studentId) {
        return studentReportRepository.findByStudentId(studentId);
    }
}
