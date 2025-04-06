package com.esvar.dekanat.mark;

import com.esvar.dekanat.dto.MarkDTO;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.service.ControlMethodService;
import com.esvar.dekanat.service.MarksService;
import com.esvar.dekanat.service.StudentService;
import com.esvar.dekanat.user.UserRepository;
import com.esvar.dekanat.security.SecurityService;

import java.sql.Timestamp;

public class ModularMarkProcessor implements MarkProcessor {

    private final MarksService marksService;
    private final UserRepository userRepository;
    private final SecurityService securityService;
    private final StudentService studentService;
    private final ControlMethodService controlMethodService;

    public ModularMarkProcessor(MarksService marksService, UserRepository userRepository, SecurityService securityService, StudentService studentService, ControlMethodService controlMethodService1) {
        this.marksService = marksService;
        this.userRepository = userRepository;
        this.securityService = securityService;
        this.studentService = studentService;
        this.controlMethodService = controlMethodService1;
    }

    @Override
    public MarksEntity processMark(MarkDTO markDTO, PlansEntity plan, String controlType) {
        MarksEntity marksEntity = new MarksEntity();
        // Отримання студента за ПІБ та групою
        marksEntity.setStudent(studentService.getStudentByStudentPIB_AndGroup(markDTO.getStudentPIB(), plan.getGroup()));
        marksEntity.setPlan(plan);
        marksEntity.setControlMethod(controlMethodService.getControlMethodByName(controlType));
        // Для спрощення цей рядок залишаємо як коментар.
        marksEntity.setSemester(plan.getSemester());
        // Для модульного контролю беремо введену оцінку
        marksEntity.setFinalGrade(Integer.parseInt(markDTO.getEnterMark()));
        marksEntity.setLocked(markDTO.isLocked());
        marksEntity.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        marksEntity.setLastUpdatedBy(
                userRepository.findByEmail(securityService.getAuthenticatedUser().getUsername()).orElseThrow()
        );
        return marksEntity;
    }
}
