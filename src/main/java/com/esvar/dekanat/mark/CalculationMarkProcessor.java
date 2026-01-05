package com.esvar.dekanat.mark;

import com.esvar.dekanat.dto.MarkDTO;
import com.esvar.dekanat.entity.ControlMethodEntity;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import com.esvar.dekanat.service.ControlMethodService;
import com.esvar.dekanat.service.MarksFacade;
import com.esvar.dekanat.service.StudentService;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CalculationMarkProcessor implements MarkProcessor {

    private final MarksFacade marksFacade;
    private final StudentService studentService;
    private final ControlMethodService controlMethodService;

    public CalculationMarkProcessor(MarksFacade marksFacade, StudentService studentService, ControlMethodService controlMethodService) {
        this.marksFacade = marksFacade;
        this.studentService = studentService;
        this.controlMethodService = controlMethodService;
    }

    @Override
    public MarksEntity processMark(MarkDTO markDTO, PlansEntity plan, StudentGroupEntity group, String controlType) {
        StudentGroupEntity targetGroup = group != null ? group : plan.getGroup();
        if (targetGroup == null) {
            throw new IllegalArgumentException("Не вдалося визначити групу для студента.");
        }
        ControlMethodEntity controlMethod = controlMethodService.getControlMethodByName(controlType);

        Map<Integer, Integer> partGrades = IntStream.rangeClosed(1, plan.getParts())
                .boxed()
                .collect(Collectors.toMap(
                        i -> i,
                        i -> {
                            String partMarkStr = getPartMarkValue(markDTO, i);
                            if (partMarkStr != null && !partMarkStr.isEmpty()) {
                                return Integer.parseInt(partMarkStr);
                            }
                            return 0;
                        }
                ));

        return marksFacade.saveCalculationMark(
                plan,
                controlMethod,
                studentService.getStudentByStudentPIB_AndGroup(markDTO.getStudentPIB(), targetGroup),
                partGrades,
                markDTO.isLocked()
        );
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
