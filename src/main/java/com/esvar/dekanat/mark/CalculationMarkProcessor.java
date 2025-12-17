package com.esvar.dekanat.mark;

import com.esvar.dekanat.dto.MarkDTO;
import com.esvar.dekanat.entity.ControlMethodEntity;
import com.esvar.dekanat.entity.ControlPartsEntity;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.MarksPartsEntity;
import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import com.esvar.dekanat.service.*;
import com.esvar.dekanat.security.SecurityService;

import java.sql.Timestamp;
import java.util.Map;
import java.util.stream.Collectors;

public class CalculationMarkProcessor implements MarkProcessor {

    private final MarksService marksService;
    private final SecurityService securityService;
    private final StudentService studentService;
    private final MarksPartsService marksPartsService;
    private final ControlMethodService controlMethodService;
    private final ControlPartsService controlPartsService;

    public CalculationMarkProcessor(MarksService marksService, SecurityService securityService,
                                    StudentService studentService, MarksPartsService marksPartsService, ControlMethodService controlMethodService, ControlPartsService controlPartsService) {
        this.marksService = marksService;
        this.securityService = securityService;
        this.studentService = studentService;
        this.marksPartsService = marksPartsService;
        this.controlMethodService = controlMethodService;
        this.controlPartsService = controlPartsService;
    }

    @Override
    public MarksEntity processMark(MarkDTO markDTO, PlansEntity plan, StudentGroupEntity group, String controlType) {
        int sum = 0;
        StudentGroupEntity targetGroup = group != null ? group : plan.getGroup();
        if (targetGroup == null) {
            throw new IllegalArgumentException("Не вдалося визначити групу для студента.");
        }
        ControlMethodEntity controlMethod = controlMethodService.getControlMethodByName(controlType);
        Map<Integer, ControlPartsEntity> partsMap =
                controlPartsService.getOrCreatePartsMap(controlMethod, plan.getParts());

        Map<Integer, Integer> partGrades = partsMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            String partMarkStr = getPartMarkValue(markDTO, entry.getKey());
                            if (partMarkStr != null && !partMarkStr.isEmpty()) {
                                return Integer.parseInt(partMarkStr);
                            }
                            return 0;
                        }
                ));
        sum = partGrades.values().stream().mapToInt(Integer::intValue).sum();

        MarksEntity marksEntity = new MarksEntity();
        marksEntity.setStudent(studentService.getStudentByStudentPIB_AndGroup(markDTO.getStudentPIB(), targetGroup));
        marksEntity.setPlan(plan);
        marksEntity.setControlMethod(controlMethod);
        marksEntity.setSemester(plan.getSemester());
        marksEntity.setLocked(markDTO.isLocked());
        marksEntity.setFinalGrade(sum);
        marksEntity.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        marksEntity.setLastUpdatedBy(
                securityService.getCurrentUserModel()
                        .orElseThrow(() -> new IllegalStateException("No authenticated user"))
        );

        MarksEntity persistedMark = marksService.saveMark(marksEntity);

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

    @Override
    public boolean isPersistedAfterProcessing() {
        return true;
    }

    // Допоміжний метод для отримання значення частини з MarkDTO
    private String getPartMarkValue(MarkDTO markDTO, int partNumber) {
        return switch (partNumber) {
            case 1 -> markDTO.getPartMark1();
            case 2 -> markDTO.getPartMark2();
            case 3 -> markDTO.getPartMark3();
            case 4 -> markDTO.getPartMark4();
            case 5 -> markDTO.getPartMark5();
            case 6 -> markDTO.getPartMark6();
            case 7 -> markDTO.getPartMark7();
            case 8 -> markDTO.getPartMark8();
            default -> "";
        };
    }

}
