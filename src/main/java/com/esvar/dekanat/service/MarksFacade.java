package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.ControlMethodEntity;
import com.esvar.dekanat.entity.ControlPartsEntity;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.MarksPartsEntity;
import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

@Service
public class MarksFacade {

    private final MarksService marksService;
    private final MarksPartsService marksPartsService;
    private final ControlPartsService controlPartsService;

    public MarksFacade(MarksService marksService, MarksPartsService marksPartsService, ControlPartsService controlPartsService) {
        this.marksService = marksService;
        this.marksPartsService = marksPartsService;
        this.controlPartsService = controlPartsService;
    }

    @Transactional
    public MarksEntity saveCalculationMark(PlansEntity plan,
                                           ControlMethodEntity controlMethod,
                                           StudentEntity student,
                                           Map<Integer, Integer> partGrades,
                                           boolean locked) {
        if (plan == null || student == null || controlMethod == null) {
            throw new IllegalArgumentException("План, студент і метод контролю повинні бути задані.");
        }
        if (partGrades == null) {
            throw new IllegalArgumentException("Оцінки частин повинні бути задані.");
        }

        int parts = plan.getParts();
        Map<Integer, ControlPartsEntity> partsMap = controlPartsService.getOrCreatePartsMap(controlMethod, parts);
        if (partGrades.size() != partsMap.size()) {
            throw new IllegalArgumentException("Кількість оцінок частин не відповідає кількості частин плану.");
        }
        if (!partGrades.keySet().containsAll(partsMap.keySet()) || !partsMap.keySet().containsAll(partGrades.keySet())) {
            throw new IllegalArgumentException("Передані номери частин не відповідають плану.");
        }

        int finalGrade = partGrades.values().stream()
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        MarksEntity mark = new MarksEntity();
        mark.setStudent(student);
        mark.setPlan(plan);
        mark.setControlMethod(controlMethod);
        mark.setSemester(plan.getSemester());
        mark.setLocked(locked);
        mark.setFinalGrade(finalGrade);

        MarksEntity persistedMark = marksService.saveMark(mark);

        for (Map.Entry<Integer, Integer> entry : partGrades.entrySet()) {
            ControlPartsEntity controlPart = partsMap.get(entry.getKey());
            MarksPartsEntity markPart = marksPartsService.getMarksPartByMarkAndPart(persistedMark, controlPart);
            if (markPart == null) {
                markPart = new MarksPartsEntity();
                markPart.setMark(persistedMark);
                markPart.setControlPart(controlPart);
            }
            markPart.setGrade(entry.getValue());
            marksPartsService.saveMarksPart(markPart);
        }

        return persistedMark;
    }
}
