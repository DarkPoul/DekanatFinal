package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.*;
import com.esvar.dekanat.repository.ControlMethodRepository;
import com.esvar.dekanat.repository.MarksPartsRepository;
import com.esvar.dekanat.repository.MarksRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MarksInitializerService {

    private static final String CONTROL_TYPE_FIRST_MODULE = "Перший модульний контроль";
    private static final String CONTROL_TYPE_SECOND_MODULE = "Другий модульний контроль";

    private final MarksRepository marksRepository;
    private final MarksPartsRepository marksPartsRepository;
    private final ControlPartsService controlPartsService;
    private final ControlMethodRepository controlMethodRepository;

    public MarksInitializerService(MarksRepository marksRepository,
                                   MarksPartsRepository marksPartsRepository,
                                   ControlPartsService controlPartsService,
                                   ControlMethodRepository controlMethodRepository) {
        this.marksRepository = marksRepository;
        this.marksPartsRepository = marksPartsRepository;
        this.controlPartsService = controlPartsService;
        this.controlMethodRepository = controlMethodRepository;
    }

    @Transactional
    public void initializeMarksForPlan(PlansEntity plan, List<StudentEntity> students) {
        if (plan == null || students == null || students.isEmpty()) {
            return;
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        ControlMethodEntity firstModuleControl = controlMethodRepository.findByName(CONTROL_TYPE_FIRST_MODULE);
        ControlMethodEntity secondModuleControl = controlMethodRepository.findByName(CONTROL_TYPE_SECOND_MODULE);
        for (StudentEntity student : students) {
            initMarkForControl(plan, student, plan.getFirstControl(), now);
            ControlMethodEntity second = plan.getSecondControl();
            if (second != null && !"Відсутній".equalsIgnoreCase(second.getName())) {
                initMarkForControl(plan, student, second, now);
            }
            initMarkForControl(plan, student, firstModuleControl, now);
            initMarkForControl(plan, student, secondModuleControl, now);
        }
    }

    private void initMarkForControl(PlansEntity plan, StudentEntity student,
                                    ControlMethodEntity method, Timestamp now) {
        if (method == null) {
            return;
        }
        MarksEntity mark = marksRepository
                .findByStudentIdAndPlanIdAndControlMethodId(
                        student.getId(),
                        plan.getId(),
                        method.getId()
                ).orElse(null);
        if (mark == null) {
            mark = new MarksEntity();
            mark.setStudent(student);
            mark.setPlan(plan);
            mark.setControlMethod(method);
            mark.setSemester(plan.getSemester());
            mark.setFinalGrade(0);
            mark.setLocked(false);
            mark.setLastUpdated(now);
            mark = marksRepository.save(mark);
        }

        Map<Integer, ControlPartsEntity> partsMap = controlPartsService.getOrCreatePartsMap(method, plan.getParts());

        List<MarksPartsEntity> existing = marksPartsRepository
                .findByMarkIdAndPartNumberLessThanEqual(mark.getId(), plan.getParts());
        Set<Long> existingPartIds = existing.stream()
                .map(mp -> mp.getControlPart().getId())
                .collect(Collectors.toSet());

        MarksEntity finalMark = mark;
        List<MarksPartsEntity> toSave = partsMap.values().stream()
                .filter(cp -> !existingPartIds.contains(cp.getId()))
                .map(cp -> {
                    MarksPartsEntity mp = new MarksPartsEntity();
                    mp.setMark(finalMark);
                    mp.setControlPart(cp);
                    mp.setGrade(0);
                    return mp;
                })
                .toList();

        if (!toSave.isEmpty()) {
            marksPartsRepository.saveAll(toSave);
        }
    }
}
