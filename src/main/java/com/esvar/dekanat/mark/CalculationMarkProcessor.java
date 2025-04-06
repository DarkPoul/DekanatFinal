package com.esvar.dekanat.mark;

import com.esvar.dekanat.dto.MarkDTO;
import com.esvar.dekanat.entity.ControlMethodEntity;
import com.esvar.dekanat.entity.ControlPartsEntity;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.MarksPartsEntity;
import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.service.*;
import com.esvar.dekanat.user.UserRepository;
import com.esvar.dekanat.security.SecurityService;

import java.sql.Timestamp;

public class CalculationMarkProcessor implements MarkProcessor {

    private final MarksService marksService;
    private final UserRepository userRepository;
    private final SecurityService securityService;
    private final StudentService studentService;
    private final MarksPartsService marksPartsService;
    private final ControlMethodService controlMethodService;
    private final ControlPartsService controlPartsService;

    public CalculationMarkProcessor(MarksService marksService, UserRepository userRepository, SecurityService securityService,
                                    StudentService studentService, MarksPartsService marksPartsService, ControlMethodService controlMethodService, ControlPartsService controlPartsService) {
        this.marksService = marksService;
        this.userRepository = userRepository;
        this.securityService = securityService;
        this.studentService = studentService;
        this.marksPartsService = marksPartsService;
        this.controlMethodService = controlMethodService;
        this.controlPartsService = controlPartsService;
    }

    @Override
    public MarksEntity processMark(MarkDTO markDTO, PlansEntity plan, String controlType) {
        MarksEntity marksEntity = new MarksEntity();
        marksEntity.setStudent(studentService.getStudentByStudentPIB_AndGroup(markDTO.getStudentPIB(), plan.getGroup()));
        marksEntity.setPlan(plan);
        // Встановіть метод контролю – адаптуйте за потребою, наприклад:
        marksEntity.setControlMethod(controlMethodService.getControlMethodByName(controlType));
        marksEntity.setSemester(plan.getSemester());
        marksEntity.setLocked(markDTO.isLocked());
        marksEntity.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        marksEntity.setLastUpdatedBy(
                userRepository.findByEmail(securityService.getAuthenticatedUser().getUsername()).orElseThrow()
        );

        // Зберігаємо MarksEntity і отримуємо managed екземпляр з ID
        marksEntity = marksService.saveMark(marksEntity);

        int sum = 0;
        for (int i = 1; i <= plan.getParts(); i++) {
            String partMarkStr = getPartMarkValue(markDTO, i);
            int partValue = 0;
            if (partMarkStr != null && !partMarkStr.isEmpty()) {
                partValue = Integer.parseInt(partMarkStr);
            }
            sum += partValue;

            // Отримуємо або створюємо ControlPartsEntity для даної частини
            ControlPartsEntity controlPart = getControlPartByNumber(i, marksEntity.getControlMethod());
            MarksPartsEntity marksPartsEntity = new MarksPartsEntity();
            marksPartsEntity.setMark(marksEntity); // marksEntity тепер managed
            marksPartsEntity.setControlPart(controlPart);
            marksPartsEntity.setGrade(partValue);
            marksPartsService.saveMarksPart(marksPartsEntity);
        }
        marksEntity.setFinalGrade(sum);
        return marksEntity;
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

    // Отримуємо ControlPartsEntity через сервіс; якщо не знайдено – створюємо новий об’єкт (значення частини вважається 0)
    private ControlPartsEntity getControlPartByNumber(int partNumber, com.esvar.dekanat.entity.ControlMethodEntity controlMethod) {
        ControlPartsEntity cp = controlPartsService.getControlPartByControlMethodAndPartNumber(controlMethod, partNumber);
        if (cp == null) {
            cp = new ControlPartsEntity();
            cp.setControlMethod(controlMethod);
            cp.setPartNumber(partNumber);
            // Зберігаємо новостворений об’єкт, щоб він став managed
            cp = controlPartsService.saveControlPart(cp);
        }
        return cp;
    }

}
