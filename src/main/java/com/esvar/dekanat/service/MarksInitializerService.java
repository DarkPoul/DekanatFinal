package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.*;
import com.esvar.dekanat.repository.MarksPartsRepository;
import com.esvar.dekanat.repository.MarksRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Service
public class MarksInitializerService {

    private final MarksRepository marksRepository;
    private final MarksPartsRepository marksPartsRepository;
    private final ControlPartsService controlPartsService;

    public MarksInitializerService(MarksRepository marksRepository,
                                   MarksPartsRepository marksPartsRepository,
                                   ControlPartsService controlPartsService) {
        this.marksRepository = marksRepository;
        this.marksPartsRepository = marksPartsRepository;
        this.controlPartsService = controlPartsService;
    }

    @Transactional
    public void initializeMarksForPlan(PlansEntity plan, List<StudentEntity> students) {
        if (plan == null || students == null || students.isEmpty()) {
            return;
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        for (StudentEntity student : students) {
            initMarkForControl(plan, student, plan.getFirstControl(), now);
            ControlMethodEntity second = plan.getSecondControl();
            if (second != null && !"Відсутній".equalsIgnoreCase(second.getName())) {
                initMarkForControl(plan, student, second, now);
            }
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

        for (int part = 1; part <= plan.getParts(); part++) {
            ControlPartsEntity cp = controlPartsService.getControlPartByControlMethodAndPartNumber(method, part);
            if (cp == null) {
                cp = new ControlPartsEntity();
                cp.setControlMethod(method);
                cp.setPartNumber(part);
                cp = controlPartsService.saveControlPart(cp);
            }
            if (!marksPartsRepository.existsByMarkIdAndControlPart(mark.getId(), cp)) {
                MarksPartsEntity mp = new MarksPartsEntity();
                mp.setMark(mark);
                mp.setControlPart(cp);
                mp.setGrade(0);
                marksPartsRepository.save(mp);
            }
        }
    }
}
