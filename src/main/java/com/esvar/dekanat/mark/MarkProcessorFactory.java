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
                                             MarksFacade marksFacade,
                                             ControlMethodService controlMethodService) {
        return switch (controlType) {
            case "Перший модульний контроль", "Другий модульний контроль", "Контрольна робота", "Залік", "Екзамен", "Курсова робота", "Курсовий проєкт", "Диференційний залік" ->
                    new ModularMarkProcessor(marksService, userRepository, securityService, studentService, controlMethodService);
            case "Розрахункова робота", "Розрахунково-графічна робота" ->
                    new CalculationMarkProcessor(marksFacade, studentService, controlMethodService);
            default -> throw new IllegalArgumentException("Unsupported control type: " + controlType);
        };
    }
}
