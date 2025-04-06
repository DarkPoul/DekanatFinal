package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.*;
import com.esvar.dekanat.repository.MarksPartsRepository;
import com.esvar.dekanat.repository.MarksRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class MarksPartsService {
    
    private final MarksPartsRepository marksPartsRepository;
    private final MarksRepository marksRepository;

    public MarksPartsService(MarksPartsRepository marksPartsRepository, MarksRepository marksRepository) {
        this.marksPartsRepository = marksPartsRepository;
        this.marksRepository = marksRepository;
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
        for (MarksEntity mark : marksList) {
            // Отримуємо всі частини оцінок, де partNumber менший або рівний newParts
            List<MarksPartsEntity> parts = marksPartsRepository.findByMarkIdAndPartNumberLessThanEqual(mark.getId(), newParts);
            // Обчислюємо суму
            int sum = parts.stream()
                    .mapToInt(mp -> mp.getGrade() != null ? mp.getGrade() : 0)
                    .sum();
            mark.setFinalGrade(sum);
            marksRepository.save(mark); // Оновлюємо запис у таблиці marks
        }
    }


}
