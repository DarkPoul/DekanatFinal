package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.*;
import com.esvar.dekanat.repository.MarksPartsRepository;
import com.esvar.dekanat.repository.MarksRepository;
import com.esvar.dekanat.security.SecurityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class MarksPartsService {
    
    private final MarksPartsRepository marksPartsRepository;
    private final MarksRepository marksRepository;
    private final ControlPartsService controlPartsService;
    private final SecurityService securityService;
    private final RatingService ratingService;

    public MarksPartsService(MarksPartsRepository marksPartsRepository,
                             MarksRepository marksRepository,
                             ControlPartsService controlPartsService,
                             SecurityService securityService,
                             RatingService ratingService) {
        this.marksPartsRepository = marksPartsRepository;
        this.marksRepository = marksRepository;
        this.controlPartsService = controlPartsService;
        this.securityService = securityService;
        this.ratingService = ratingService;
    }

    public int getNumberOfPartsForPlan(PlansEntity plan) {
        if (plan == null || plan.getSecondControl() == null) {
            return 0; // Якщо план або другий контроль відсутні, повертаємо 0
        }

        // Отримуємо всі записи marks_parts для даного плану
        Set<Integer> partNumbers = marksPartsRepository.findDistinctPartNumbersByPlanId(plan.getId());

        // Повертаємо розмір множини унікальних номерів частин
        return partNumbers.size();
    }

    /**
     * Зберігає новий запис у marks_parts або оновлює існуючий.
     *
     * @param marksPart MarksPartsEntity - об'єкт для збереження.
     */
    public void saveMarksPart(MarksPartsEntity marksPart) {
        if (marksPart == null || marksPart.getMark() == null || marksPart.getControlPart() == null) {
            throw new IllegalArgumentException("Оцінка та частина повинні бути задані.");
        }
        ControlPartsEntity controlPart = marksPart.getControlPart();

        // Спробуємо знайти існуючий запис для цієї оцінки та цієї частини
        Optional<MarksPartsEntity> existingOptional = marksPartsRepository.findByMarkIdAndPartId(
                marksPart.getMark().getId(),
                controlPart.getId()
        );

        if (existingOptional.isPresent()) {
            // Якщо запис існує, оновлюємо його значення
            MarksPartsEntity existing = existingOptional.get();
            existing.setGrade(marksPart.getGrade());
            marksPartsRepository.save(existing);
        } else {
            // Якщо запис не існує, зберігаємо новий
            marksPartsRepository.save(marksPart);
        }
    }


    /**
     * Видаляє всі записи у marks_parts для певного плану.
     *
     * @param updatedPlan PlansEntity - план, для якого потрібно видалити записи.
     */
    @Transactional
    public void deleteMarksPartsByPlan(PlansEntity updatedPlan) {
        if (updatedPlan == null || updatedPlan.getId() == null) {
            throw new IllegalArgumentException("План для видалення повинен бути заданий.");
        }

        // Видаляємо всі записи, пов'язані з планом
        marksPartsRepository.deleteByPlanId(updatedPlan.getId());
    }

    @Transactional
    public void deleteByPlanId(Long planId) {
        if (planId != null) {
            marksPartsRepository.deleteByPlanId(planId);
        }
    }

    public void saveAll(List<MarksPartsEntity> parts) {
        if (parts == null || parts.isEmpty()) {
            return;
        }
        marksPartsRepository.saveAll(parts);
    }

    @Transactional
    public void deleteByPlanIdAndStudentIds(Long planId, List<Long> studentIds) {
        if (planId != null && studentIds != null && !studentIds.isEmpty()) {
            marksPartsRepository.deleteByPlanIdAndStudentIds(planId, studentIds);
        }
    }

    /**
     * Отримує запис у marks_parts за оцінкою та частиною.
     *
     * @param existingMark   MarksEntity - оцінка.
     * @param existingPart   ControlPartsEntity - частина.
     * @return MarksPartsEntity - знайдений запис або null, якщо не знайдено.
     */
    public MarksPartsEntity getMarksPartByMarkAndPart(MarksEntity existingMark, ControlPartsEntity existingPart) {
        if (existingMark == null || existingPart == null) {
            return null; // Якщо оцінка або частина відсутні, повертаємо null
        }

        return marksPartsRepository.findByMarkIdAndPartId(existingMark.getId(), existingPart.getId()).orElse(null);
    }

    @Transactional
    public void deletePartsGreaterThan(Long planId, int newParts) {
        marksPartsRepository.deleteByPlanIdAndPartNumberGreaterThan(planId, newParts);
    }

    @Transactional
    public void updateFinalGradesForPlan(PlansEntity plan, int newParts) {
        List<MarksEntity> marksList = marksRepository.findByPlan(plan);
        Set<Long> studentIds = new HashSet<>();
        for (MarksEntity mark : marksList) {
            // Отримуємо всі частини оцінок, де partNumber менший або рівний newParts
            List<MarksPartsEntity> parts = marksPartsRepository.findByMarkIdAndPartNumberLessThanEqual(mark.getId(), newParts);
            // Обчислюємо суму
            int sum = parts.stream()
                    .mapToInt(mp -> mp.getGrade() != null ? mp.getGrade() : 0)
                    .sum();
            mark.setFinalGrade(sum);
            mark.setLastUpdated(new java.sql.Timestamp(System.currentTimeMillis()));
            mark.setLastUpdatedBy(
                    securityService.getCurrentUserModel()
                            .orElseThrow(() -> new IllegalStateException("No authenticated user"))
            );
            studentIds.add(mark.getStudent().getId());
            marksRepository.save(mark); // Оновлюємо запис у таблиці marks
        }
        ratingService.updateRatingsForStudentIds(studentIds);
    }

    /**
     * When the control method of a plan changes, move existing marks and their
     * parts to the new method so that scores are preserved.
     *
     * @param plan       plan that was updated
     * @param oldMethod  previous control method
     * @param newMethod  new control method
     */
    @Transactional
    public void transferControlMethod(PlansEntity plan,
                                      ControlMethodEntity oldMethod,
                                      ControlMethodEntity newMethod) {
        if (plan == null || oldMethod == null || newMethod == null) {
            return;
        }
        if (oldMethod.getId().equals(newMethod.getId())) {
            return; // nothing to do
        }

        List<MarksEntity> marks = marksRepository.findByPlanAndControlMethod(plan, oldMethod);
        if (marks.isEmpty()) {
            return;
        }

        var newParts = controlPartsService.getOrCreatePartsMap(newMethod, plan.getParts());
        for (MarksEntity mark : marks) {
            mark.setControlMethod(newMethod);
            mark.setLastUpdated(new java.sql.Timestamp(System.currentTimeMillis()));
            mark.setLastUpdatedBy(
                    securityService.getCurrentUserModel()
                            .orElseThrow(() -> new IllegalStateException("No authenticated user"))
            );
            marksRepository.save(mark);

            List<MarksPartsEntity> parts = marksPartsRepository
                    .findByMarkIdAndPartNumberLessThanEqual(mark.getId(), plan.getParts());
            for (MarksPartsEntity mp : parts) {
                int partNumber = mp.getControlPart().getPartNumber();
                ControlPartsEntity targetPart = newParts.get(partNumber);
                if (targetPart != null) {
                    mp.setControlPart(targetPart);
                    marksPartsRepository.save(mp);
                }
            }
        }
    }


}
