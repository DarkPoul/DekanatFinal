package com.esvar.dekanat.service;

import com.esvar.dekanat.dto.MarkDTO;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.repository.MarksRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MarksService {

    private final MarksRepository marksRepository;

    public MarksService(MarksRepository marksRepository) {
        this.marksRepository = marksRepository;
    }

    /**
     * Зберігає нову оцінку.
     *
     * @param mark MarksEntity - об'єкт для збереження.
     */
    @Transactional
    public void saveMark(MarksEntity mark) {
        if (mark == null || mark.getStudent() == null || mark.getPlan() == null || mark.getControlMethod() == null) {
            throw new IllegalArgumentException("Студент, план і метод контролю повинні бути задані.");
        }

        // Перевіряємо, чи існує вже така оцінка (унікальність за student_id, plan_id, control_method_id)
        boolean exists = marksRepository.existsByStudentIdAndPlanIdAndControlMethodId(
                mark.getStudent().getId(),
                mark.getPlan().getId(),
                mark.getControlMethod().getId()
        );

        if (exists) {
            MarksEntity marksEntity = marksRepository.findByStudentIdAndPlanIdAndControlMethodId(
                    mark.getStudent().getId(),
                    mark.getPlan().getId(),
                    mark.getControlMethod().getId()
            );

            marksEntity.setFinalGrade(mark.getFinalGrade());
            marksEntity.setLocked(mark.isLocked());
            marksEntity.setLastUpdated(mark.getLastUpdated());
            marksEntity.setLastUpdatedBy(mark.getLastUpdatedBy());

            marksRepository.save(marksEntity);

        } else marksRepository.save(mark);
    }

    /**
     * Отримує оцінку за студента та план.
     *
     * @param student   StudentEntity - студент.
     * @param updatedPlan PlansEntity - план.
     * @return MarksEntity - знайдена оцінка або null, якщо не знайдено.
     */
    public MarksEntity getMarkByStudentAndPlan(StudentEntity student, PlansEntity updatedPlan) {
        if (student == null || updatedPlan == null) {
            return null; // Якщо студент або план відсутні, повертаємо null
        }

        return marksRepository.findByStudentIdAndPlanId(student.getId(), updatedPlan.getId()).orElse(null);
    }

    public Long getLastId() {
        return marksRepository.findMaxId().orElse(0L);
    }

    public List<MarksEntity> findMarksByPlan(PlansEntity plansEntity) {
        return marksRepository.findByPlan(plansEntity);
    }

    public MarksEntity getMarkById(Long id) {
        return marksRepository.findById(id).orElse(null);
    }
}
