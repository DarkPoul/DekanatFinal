package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.*;
import com.esvar.dekanat.repository.MarksPartsRepository;
import com.esvar.dekanat.repository.MarksRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // Перевіряємо, чи запис вже існує
        boolean exists = marksPartsRepository.existsByMarkIdAndControlPart(
                marksPart.getMark().getId(),
                controlPart
        );

        if (exists) {
            throw new IllegalStateException("Запис для даної оцінки та частини вже існує.");
        }

        // Зберігаємо новий запис
        marksPartsRepository.save(marksPart);
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
}
