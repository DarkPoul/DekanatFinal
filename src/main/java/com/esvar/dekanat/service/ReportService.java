package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.ReportEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public void saveReport(ReportEntity reportEntity) {
        if (reportEntity.getId() == null) {
            // Отримуємо максимальний ID з бази даних
            Long maxId = reportRepository.findMaxId();
            Long newId = (maxId != null) ? maxId + 1 : 1; // Якщо таблиця порожня, починаємо з 1
            reportEntity.setId(newId); // Встановлюємо новий ID
        }
        reportRepository.save(reportEntity);
    }

    public List<ReportEntity> getReports(StudentEntity studentEntity) {
        return reportRepository.findAllByStudent(studentEntity);
    }
}
