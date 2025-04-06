package com.esvar.dekanat.mark;

import com.esvar.dekanat.service.*;
import com.esvar.dekanat.user.UserRepository;
import com.esvar.dekanat.security.SecurityService;

public class MarkProcessorFactory {

    public static MarkProcessor getProcessor(String controlType,
                                             MarksService marksService,
                                             UserRepository userRepository,
                                             SecurityService securityService,
                                             StudentService studentService,
                                             MarksPartsService marksPartsService,
                                             ControlMethodService controlMethodService,
                                             ControlPartsService controlPartsService) {
        return switch (controlType) {
            case "Перший модульний контроль", "Другий модульний контроль", "Залік", "Екзамен", "Курсова робота", "Курсовий проєкт" ->
                    new ModularMarkProcessor(marksService, userRepository, securityService, studentService, controlMethodService);
            case "Розрахункова робота", "Розрахунково-графічна робота" ->
                    new CalculationMarkProcessor(marksService, userRepository, securityService, studentService, marksPartsService, controlMethodService, controlPartsService);
            default -> throw new IllegalArgumentException("Unsupported control type: " + controlType);
        };
    }
}
