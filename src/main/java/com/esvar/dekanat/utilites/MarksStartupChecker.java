package com.esvar.dekanat.utilites;

import com.esvar.dekanat.entity.*;
import com.esvar.dekanat.repository.MarksPartsRepository;
import com.esvar.dekanat.repository.MarksRepository;
import com.esvar.dekanat.repository.StudentPlansRepository;
import com.esvar.dekanat.service.ControlPartsService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

//@Component
public class MarksStartupChecker implements ApplicationRunner {

    private final StudentPlansRepository studentPlansRepository;
    private final MarksRepository marksRepository;
    private final MarksPartsRepository marksPartsRepository;
    private final ControlPartsService controlPartsService;

    public MarksStartupChecker(StudentPlansRepository studentPlansRepository,
                               MarksRepository marksRepository,
                               MarksPartsRepository marksPartsRepository,
                               ControlPartsService controlPartsService) {
        this.studentPlansRepository = studentPlansRepository;
        this.marksRepository = marksRepository;
        this.marksPartsRepository = marksPartsRepository;
        this.controlPartsService = controlPartsService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<StudentPlansEntity> studentPlans = studentPlansRepository.findAll();
        for (StudentPlansEntity sp : studentPlans) {
            PlansEntity plan = sp.getPlan();
            checkMarks(sp, plan, plan.getFirstControl(), now);
            ControlMethodEntity second = plan.getSecondControl();
            if (second != null && !"Відсутній".equalsIgnoreCase(second.getName())) {
                checkMarks(sp, plan, second, now);
            }
        }
    }

    private void checkMarks(StudentPlansEntity sp, PlansEntity plan, ControlMethodEntity method, Timestamp now) {
        if (method == null) {
            return;
        }
        MarksEntity mark = marksRepository
                .findByStudentIdAndPlanIdAndControlMethodId(
                        sp.getStudent().getId(),
                        plan.getId(),
                        method.getId()
                ).orElse(null);
        if (mark == null) {
            mark = new MarksEntity();
            mark.setStudent(sp.getStudent());
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
