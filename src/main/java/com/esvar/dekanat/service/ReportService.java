package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.ReportEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Collection;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public ReportEntity saveReport(ReportEntity reportEntity) {
        if (reportEntity.getId() == null) {
            // Отримуємо максимальний ID з бази даних
            Long maxId = reportRepository.findMaxId();
            Long newId = (maxId != null) ? maxId + 1 : 1; // Якщо таблиця порожня, починаємо з 1
            reportEntity.setId(newId); // Встановлюємо новий ID
        }
        return reportRepository.save(reportEntity);
    }

    public List<ReportEntity> getReports(StudentEntity studentEntity) {
        return reportRepository.findAllByStudent(studentEntity);
    }

    public Long getNextOrderNumber() {
        Long maxOrder = reportRepository.findMaxOrderNumber();
        return (maxOrder != null) ? maxOrder + 1 : 1;
    }

    public ReportEntity archiveStudent(StudentEntity student) {
        ReportEntity archiveRecord = new ReportEntity();
        archiveRecord.setStudent(student);
        archiveRecord.setStatus("Відправлено в архів");
        archiveRecord.setDate(new java.sql.Date(System.currentTimeMillis()));
        archiveRecord.setOrderNumber(getNextOrderNumber());
        return saveReport(archiveRecord);
    }

    @Transactional
    public void deleteReportsByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        reportRepository.deleteAllById(ids);
    }
}
