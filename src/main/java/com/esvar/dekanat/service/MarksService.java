package com.esvar.dekanat.service;

import com.esvar.dekanat.dto.MarkDTO;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.repository.ControlMethodRepository;
import com.esvar.dekanat.repository.MarksRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MarksService {

    private final MarksRepository marksRepository;
    private final ControlMethodRepository controlMethodRepository;

    public MarksService(MarksRepository marksRepository, ControlMethodRepository controlMethodRepository) {
        this.marksRepository = marksRepository;
        this.controlMethodRepository = controlMethodRepository;
    }

    /**
     * Зберігає нову оцінку.
     *
     * @param mark MarksEntity - об'єкт для збереження.
     */
    @Transactional
    public MarksEntity saveMark(MarksEntity mark) {
        if (mark == null || mark.getStudent() == null || mark.getPlan() == null || mark.getControlMethod() == null) {
            throw new IllegalArgumentException("Студент, план і метод контролю повинні бути задані.");
        }
        boolean exists = marksRepository.existsByStudentIdAndPlanIdAndControlMethodId(
                mark.getStudent().getId(),
                mark.getPlan().getId(),
                mark.getControlMethod().getId()
        );

        if (exists) {
            Optional<MarksEntity>  existingOptional = marksRepository.findByStudentIdAndPlanIdAndControlMethodId(
                    mark.getStudent().getId(),
                    mark.getPlan().getId(),
                    mark.getControlMethod().getId()
            );

            MarksEntity existing = existingOptional.orElseThrow(() -> new IllegalArgumentException("Оцінка не знайдена."));

            existing.setFinalGrade(mark.getFinalGrade());
            existing.setLocked(mark.isLocked());
            existing.setLastUpdated(mark.getLastUpdated());
            existing.setLastUpdatedBy(mark.getLastUpdatedBy());
            return marksRepository.save(existing);
        } else {
            return marksRepository.save(mark);
        }
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

    public List<MarksEntity> findMarksByPlanAndTypeControl(PlansEntity plansEntity, String typeControl) {
        return marksRepository.findByPlanAndControlMethod(plansEntity, controlMethodRepository.findByName(typeControl));
    }

    public MarksEntity getMarkById(Long id) {
        return marksRepository.findById(id).orElse(null);
    }

    public String getMarkForFirstModalControl(StudentEntity studentEntity, PlansEntity plansEntity, String typeControl) {
        Optional<MarksEntity> opt = marksRepository.findByStudentIdAndPlanIdAndControlMethodId(
                studentEntity.getId(),
                plansEntity.getId(),
                controlMethodRepository.findByName(typeControl).getId()
        );
        if (opt.isPresent() && opt.get().getFinalGrade() != 0) {
            return String.valueOf(opt.get().getFinalGrade());
        }
        return "0";
    }

}
