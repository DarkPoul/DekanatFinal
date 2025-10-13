package com.esvar.dekanat.service;

import com.esvar.dekanat.dto.MarkDTO;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.repository.ControlMethodRepository;
import com.esvar.dekanat.repository.MarksRepository;
import com.esvar.dekanat.security.SecurityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
public class MarksService {

    private final MarksRepository marksRepository;
    private final ControlMethodRepository controlMethodRepository;
    private final RatingService ratingService;
    private final SecurityService securityService;

    public MarksService(MarksRepository marksRepository, ControlMethodRepository controlMethodRepository, RatingService ratingService, SecurityService securityService) {
        this.marksRepository = marksRepository;
        this.controlMethodRepository = controlMethodRepository;
        this.ratingService = ratingService;
        this.securityService = securityService;
    }

    /**
     * Зберігає нову оцінку.
     *
     * @param mark MarksEntity - об'єкт для збереження.
     */
    @Transactional
    public MarksEntity saveMark(MarksEntity mark) {
        if (mark == null || mark.getStudent() == null || mark.getPlan() == null || mark.getControlMethod() == null) {
            System.out.println("Invalid mark data: " + mark);
            System.out.println("Student: " + (mark != null ? mark.getStudent() : "null"));
            System.out.println("Plan: " + (mark != null ? mark.getPlan() : "null"));
            System.out.println("Control Method: " + (mark != null ? mark.getControlMethod() : "null"));
            throw new IllegalArgumentException("Студент, план і метод контролю повинні бути задані.");
        }
        mark.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        mark.setLastUpdatedBy(
                securityService.getCurrentUserModel()
                        .orElseThrow(() -> new IllegalStateException("No authenticated user"))
        );
        boolean exists = marksRepository.existsByStudentIdAndPlanIdAndControlMethodId(
                mark.getStudent().getId(),
                mark.getPlan().getId(),
                mark.getControlMethod().getId()
        );
        MarksEntity saved;
        if (exists) {
            Optional<MarksEntity> existingOptional = marksRepository.findByStudentIdAndPlanIdAndControlMethodId(
                    mark.getStudent().getId(),
                    mark.getPlan().getId(),
                    mark.getControlMethod().getId()
            );

            MarksEntity existing = existingOptional.orElseThrow(() -> new IllegalArgumentException("Оцінка не знайдена."));

            existing.setFinalGrade(mark.getFinalGrade());
            existing.setLocked(mark.isLocked());
            existing.setLastUpdated(mark.getLastUpdated());
            existing.setLastUpdatedBy(mark.getLastUpdatedBy());
            saved = marksRepository.save(existing);
        } else {
            saved = marksRepository.save(mark);
        }
        ratingService.updateRatingForStudent(saved.getStudent());
        return saved;
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

    /**
     * Returns all marks for the given student.
     *
     * @param student StudentEntity - student for which marks are requested.
     * @return list of MarksEntity
     */
    public List<MarksEntity> getMarksByStudent(StudentEntity student) {
        if (student == null) {
            return List.of();
        }
        return marksRepository.findByStudentId(student.getId());
    }

    /**
     * Get mark by its id.
     *
     * @param id identifier of mark
     * @return MarksEntity or null if not found
     */
    public MarksEntity getMarkById(Long id) {
        return marksRepository.findById(id).orElse(null);
    }

    /**
     * Batch save for a list of marks.
     * Each mark is processed via {@link #saveMark(MarksEntity)}.
     *
     * @param marks list of entities to save
     */
    @Transactional
    public void saveMarks(List<MarksEntity> marks) {
        if (marks == null || marks.isEmpty()) {
            return;
        }
        for (MarksEntity mark : marks) {
            saveMark(mark);
        }
    }

    @Transactional
    public void deleteByPlanId(Long planId) {
        if (planId != null) {
            marksRepository.deleteByPlanId(planId);
        }
    }

    @Transactional
    public void deleteByPlanIdAndStudentIds(Long planId, List<Long> studentIds) {
        if (planId != null && studentIds != null && !studentIds.isEmpty()) {
            marksRepository.deleteByPlanIdAndStudentIds(planId, studentIds);
        }
    }

}
