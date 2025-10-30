package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import com.esvar.dekanat.entity.StudentPlansEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for synchronizing plans between students.
 */
@Service
public class SyncService {

    private final StudentPlansService studentPlansService;
    private final PlanService planService;
    private final MarksInitializerService marksInitializerService;

    public SyncService(StudentPlansService studentPlansService,
                       PlanService planService,
                       MarksInitializerService marksInitializerService) {
        this.studentPlansService = studentPlansService;
        this.planService = planService;
        this.marksInitializerService = marksInitializerService;
    }

    /**
     * Copy all study plans from source student to focus on a student.
     * Created plans are exact copies with grades initialized to zero.
     *
     * @param target студент, до якого копіюємо
     * @param source студент, з якого копіюємо
     */
    public void synchronize(StudentEntity target, StudentEntity source) {
        List<StudentPlansEntity> sourcePlans = studentPlansService.getPlansForStudent(source);
        for (StudentPlansEntity sp : sourcePlans) {
            PlansEntity srcPlan = sp.getPlan();
            PlansEntity newPlan = new PlansEntity();
            newPlan.setSpecialty(srcPlan.getSpecialty());
            newPlan.setDiscipline(srcPlan.getDiscipline());
            newPlan.setDepartment(srcPlan.getDepartment());
            newPlan.setSemester(srcPlan.getSemester());
            newPlan.setHours(srcPlan.getHours());
            newPlan.setElective(srcPlan.isElective());
            newPlan.setParts(srcPlan.getParts());
            newPlan.setFirstControl(srcPlan.getFirstControl());
            newPlan.setSecondControl(srcPlan.getSecondControl());
            newPlan.setFaculty(srcPlan.getFaculty());
            StudentGroupEntity targetGroup = target.getGroup();
            newPlan.setGroup(targetGroup);
            newPlan.addGroup(targetGroup);
            planService.savePlan(newPlan);

            StudentPlansEntity mapping = new StudentPlansEntity();
            mapping.setStudent(target);
            mapping.setPlan(newPlan);
            studentPlansService.saveStudentPlan(mapping);

            marksInitializerService.initializeMarksForPlan(newPlan, List.of(target));
        }
    }
}
