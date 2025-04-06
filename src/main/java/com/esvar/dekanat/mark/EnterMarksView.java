package com.esvar.dekanat.mark;

import com.esvar.dekanat.dto.MarkDTO;
import com.esvar.dekanat.entity.*;
import com.esvar.dekanat.security.SecurityService;
import com.esvar.dekanat.service.*;
import com.esvar.dekanat.user.UserRepository;
import com.esvar.dekanat.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@PageTitle("Введення оцінок | Деканат")
@Route(value = "marks", layout = MainLayout.class)
@PermitAll
public class EnterMarksView extends Div {

    private final FacultyService facultyService;
    private final DepartmentService departmentService;
    private final PlanService planService;
    private final StudentService studentService;
    private final StudentPlansService studentPlansService;
    private final SecurityService securityService;
    private final UserRepository userRepository;
    private final MarksService marksService;
    private final ControlMethodService controlMethodService;
    private final MarksPartsService marksPartsService;
    private final ControlPartsService controlPartsService;

    private VerticalLayout mainLayout = new VerticalLayout();
    private HorizontalLayout contentLayout = new HorizontalLayout();
    private VerticalLayout leftLayout = new VerticalLayout();
    private VerticalLayout rightLayout = new VerticalLayout();
    private HorizontalLayout buttonLayout = new HorizontalLayout();

    private Select<String> selectFaculty = new Select<>();
    private Select<String> selectDepartment = new Select<>();
    private Select<String> selectSpecialty = new Select<>();
    private Select<String> selectCourse = new Select<>();
    private Select<String> selectGroup = new Select<>();
    private Select<String> selectDiscipline = new Select<>();
    private Select<String> selectControlType = new Select<>();
    private PlansEntity plansEntity = new PlansEntity();
    private Grid<MarkDTO> studentGrid = new Grid<>(MarkDTO.class, false);

    public EnterMarksView(FacultyService facultyService, DepartmentService departmentService, PlanService planService,
                          StudentService studentService, StudentPlansService studentPlansService, SecurityService securityService,
                          UserRepository userRepository, MarksService marksService, ControlMethodService controlMethodService,
                          MarksPartsService marksPartsService, ControlPartsService controlPartsService) {
        this.facultyService = facultyService;
        this.departmentService = departmentService;
        this.planService = planService;
        this.studentService = studentService;
        this.studentPlansService = studentPlansService;
        this.securityService = securityService;
        this.userRepository = userRepository;
        this.marksService = marksService;
        this.controlMethodService = controlMethodService;
        this.marksPartsService = marksPartsService;
        this.controlPartsService = controlPartsService;

        // Налаштування форми вибору параметрів
        selectFaculty.setLabel("Факультет");
        selectFaculty.setWidth("100%");
        selectFaculty.getStyle().set("padding", "0px").set("margin", "0px").set("margin-bottom", "5px");
        selectFaculty.setItems(facultyService.getFacultyTitles());

        selectDepartment.setLabel("Кафедра");
        selectDepartment.setWidth("100%");
        selectDepartment.getStyle().set("padding", "0px").set("margin", "0px").set("margin-bottom", "5px");
        selectDepartment.setItems(departmentService.getAllDepartment());

        selectSpecialty.setLabel("Спеціальність");
        selectSpecialty.setWidth("100%");
        selectSpecialty.getStyle().set("padding", "0px").set("margin", "0px").set("margin-bottom", "5px");

        selectCourse.setLabel("Курс");
        selectCourse.setWidth("100%");
        selectCourse.getStyle().set("padding", "0px").set("margin", "0px").set("margin-bottom", "5px");

        selectGroup.setLabel("Група");
        selectGroup.setWidth("100%");
        selectGroup.getStyle().set("padding", "0px").set("margin", "0px").set("margin-bottom", "5px");

        selectDiscipline.setLabel("Дисципліна");
        selectDiscipline.setWidth("100%");
        selectDiscipline.getStyle().set("padding", "0px").set("margin", "0px").set("margin-bottom", "5px");

        selectControlType.setLabel("Вид контролю");
        selectControlType.setWidth("100%");
        selectControlType.getStyle().set("padding", "0px").set("margin", "0px").set("margin-bottom", "5px");

        leftLayout.add(selectFaculty, selectDepartment, selectSpecialty, selectCourse, selectGroup, selectDiscipline, selectControlType);
        leftLayout.getStyle().set("padding-top", "0px");
        leftLayout.getStyle().set("gap", "5px");
        leftLayout.getStyle().set("padding-left", "0px");

        // Налаштування кнопок
        Button saveButton = new Button("Зберегти", new Icon(VaadinIcon.CLIPBOARD_CHECK));
        Button approveButton = new Button("Затвердити", new Icon(VaadinIcon.CHECK_CIRCLE));
        Button unlockButton = new Button("Розблокувати", new Icon(VaadinIcon.UNLOCK));
        Button printReportButton = new Button("Друк відомості", new Icon(VaadinIcon.PRINT));
        Button additionalReportButton = new Button("Додаткова відомість", new Icon(VaadinIcon.FILE_ADD));

        buttonLayout.add(saveButton, approveButton, unlockButton, printReportButton, additionalReportButton);
        buttonLayout.setWidth("100%");
        buttonLayout.setFlexGrow(1, saveButton, approveButton, unlockButton, printReportButton, additionalReportButton);
        buttonLayout.getStyle().set("gap", "10px");

        // Налаштування таблиці студентів
        studentGrid.getStyle().set("border-radius", "8px");
        studentGrid.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        studentGrid.getStyle().set("position", "relative");
        studentGrid.getStyle().set("background-color", "white");
        studentGrid.getStyle().set("padding", "16px");

        rightLayout.add(buttonLayout, studentGrid);
        rightLayout.getStyle().set("height", "calc(100vh - 80px)");
        rightLayout.setWidthFull();

        VerticalLayout leftContainer = new VerticalLayout(leftLayout);
        leftContainer.setWidth("20%");
        leftContainer.setPadding(false);

        contentLayout.add(leftContainer, rightLayout);
        contentLayout.setWidthFull();
        contentLayout.getStyle().set("height", "calc(100vh - 80px)");

        mainLayout.add(contentLayout);
        mainLayout.getStyle().set("height", "calc(100vh - 80px)");
        add(mainLayout);

        selectDepartment.setReadOnly(true);
        selectSpecialty.setReadOnly(true);
        selectCourse.setReadOnly(true);
        selectGroup.setReadOnly(true);
        selectDiscipline.setReadOnly(true);
        selectControlType.setReadOnly(true);

        // Обробники подій для селектів
        selectFaculty.addValueChangeListener(event -> {
            clearGrid();
            selectDepartment.setReadOnly(false);
            selectSpecialty.setReadOnly(true);
            selectCourse.setReadOnly(true);
            selectGroup.setReadOnly(true);
            selectDiscipline.setReadOnly(true);
            selectControlType.setReadOnly(true);

            selectDepartment.clear();
            selectSpecialty.clear();
            selectCourse.clear();
            selectGroup.clear();
            selectDiscipline.clear();
            selectControlType.clear();
        });

        selectDepartment.addValueChangeListener(event -> {
            clearGrid();
            if (selectDepartment.getValue() != null) {
                selectSpecialty.setReadOnly(false);
                selectCourse.setReadOnly(true);
                selectGroup.setReadOnly(true);
                selectDiscipline.setReadOnly(true);
                selectControlType.setReadOnly(true);

                selectCourse.clear();
                selectGroup.clear();
                selectDiscipline.clear();
                selectControlType.clear();

                selectSpecialty.setItems(planService.getSpecialtiesByFacultyAndDepartment(selectFaculty.getValue(), selectDepartment.getValue()));
            }
        });

        selectSpecialty.addValueChangeListener(event -> {
            clearGrid();
            if (selectSpecialty.getValue() != null) {
                selectCourse.setReadOnly(false);
                selectGroup.setReadOnly(true);
                selectDiscipline.setReadOnly(true);
                selectControlType.setReadOnly(true);

                selectGroup.clear();
                selectDiscipline.clear();
                selectControlType.clear();

                selectCourse.setItems(planService.getCourseByFacultyAndDepartmentAndSpecialty(
                        selectFaculty.getValue(),
                        selectDepartment.getValue(),
                        selectSpecialty.getValue()
                ));
            }
        });

        selectCourse.addValueChangeListener(event -> {
            clearGrid();
            if (selectCourse.getValue() != null) {
                selectGroup.setReadOnly(false);
                selectDiscipline.setReadOnly(true);
                selectControlType.setReadOnly(true);

                selectDiscipline.clear();
                selectControlType.clear();

                selectGroup.setItems(planService.getNumGroupsByFacultyAndDepartmentAndSpecialtyAndCourse(
                        selectFaculty.getValue(),
                        selectDepartment.getValue(),
                        selectSpecialty.getValue(),
                        Integer.parseInt(selectCourse.getValue())
                ));
            }
        });

        selectGroup.addValueChangeListener(event -> {
            clearGrid();
            if (selectGroup.getValue() != null) {
                selectDiscipline.setReadOnly(false);
                selectControlType.setReadOnly(true);

                selectControlType.clear();

                selectDiscipline.setItems(planService.getDisciplinesByFacultyAndDepartmentAndSpecialtyAndGroupCourseAndGroupGroupNumber(
                        selectFaculty.getValue(),
                        selectDepartment.getValue(),
                        selectSpecialty.getValue(),
                        Integer.parseInt(selectCourse.getValue()),
                        Integer.parseInt(selectGroup.getValue())
                ));
            }
        });

        selectDiscipline.addValueChangeListener(event -> {
            clearGrid();
            if (selectDiscipline.getValue() != null) {
                selectControlType.setReadOnly(false);

                selectControlType.setItems(planService.getControlTypesByFacultyAndDepartmentAndSpecialtyAndGroupCourseAndGroupNumberAndDiscipline(
                        selectFaculty.getValue(),
                        selectDepartment.getValue(),
                        selectSpecialty.getValue(),
                        Integer.parseInt(selectCourse.getValue()),
                        Integer.parseInt(selectGroup.getValue()),
                        selectDiscipline.getValue()
                ));

                plansEntity = planService.getPlanEntityByFacultyAndDepartmentAndSpecialtyAndGroupCourseAndGroupNumberAndDiscipline(
                        selectFaculty.getValue(),
                        selectDepartment.getValue(),
                        selectSpecialty.getValue(),
                        Integer.parseInt(selectCourse.getValue()),
                        Integer.parseInt(selectGroup.getValue()),
                        selectDiscipline.getValue()
                );
            }
        });

        selectControlType.addValueChangeListener(event -> updateGrid());

        // Обробник кнопки "Зберегти" із використанням фабрики процесорів
        saveButton.addClickListener(event -> {
            List<MarkDTO> markDTOList = new ArrayList<>();
            studentGrid.getDataProvider().fetch(new Query<>()).forEach(markDTOList::add);

            String controlType = selectControlType.getValue();
            MarkProcessor processor = MarkProcessorFactory.getProcessor(controlType, marksService, userRepository,
                    securityService, studentService, marksPartsService,controlMethodService, controlPartsService);

            for (MarkDTO markDTO : markDTOList) {
                MarksEntity marksEntity = processor.processMark(markDTO, plansEntity, controlType);
                marksService.saveMark(marksEntity);
            }
            updateGrid();
        });

        // Обробники кнопок "Затвердити" та "Розблокувати"
        approveButton.addClickListener(event -> {
            List<MarkDTO> markDTOList = new ArrayList<>();
            studentGrid.getDataProvider().fetch(new Query<>()).forEach(markDTOList::add);
            for (MarkDTO markDTO : markDTOList) {
                MarksEntity marksEntity = marksService.getMarkById(markDTO.getId());
                marksEntity.setLastUpdated(new Timestamp(System.currentTimeMillis()));
                marksEntity.setLastUpdatedBy(userRepository.findByEmail(securityService.getAuthenticatedUser().getUsername()).orElseThrow());
                setLocked(marksEntity);
                updateGrid();
            }
        });

        unlockButton.addClickListener(event -> {
            List<MarkDTO> markDTOList = new ArrayList<>();
            studentGrid.getDataProvider().fetch(new Query<>()).forEach(markDTOList::add);
            for (MarkDTO markDTO : markDTOList) {
                MarksEntity marksEntity = marksService.getMarkById(markDTO.getId());
                marksEntity.setLastUpdated(new Timestamp(System.currentTimeMillis()));
                marksEntity.setLastUpdatedBy(userRepository.findByEmail(securityService.getAuthenticatedUser().getUsername()).orElseThrow());
                marksEntity.setLocked(false);
                marksService.saveMark(marksEntity);
                updateGrid();
            }
        });
    }

    private void configureGrid(String typeControl, int part) {
        studentGrid.removeAllColumns();
        studentGrid.addColumn(student -> String.valueOf(studentGrid.getListDataView().getItems().toList().indexOf(student) + 1))
                .setHeader("№")
                .setFlexGrow(1).setWidth("25px");
        studentGrid.addColumn(MarkDTO::getStudentPIB)
                .setHeader("ПІБ студента")
                .setFlexGrow(3).setWidth("250px");

        if (typeControl.equals("Перший модульний контроль")) {
            setEnterMarkColumn();
        }
        if (typeControl.equals("Другий модульний контроль")) {
            studentGrid.addColumn(MarkDTO::getMarkByFirstModule)
                    .setHeader("Перший модуль").setAutoWidth(true);
            setEnterMarkColumn();
            studentGrid.addColumn(MarkDTO::getTotalMarkByFirstAndSecondModule)
                    .setHeader("Сума за модулі").setAutoWidth(true);
        }
        if (typeControl.equals("Залік") ||
                typeControl.equals("Екзамен") ||
                typeControl.equals("Курсова робота") ||
                typeControl.equals("Курсовий проєкт")) {
            setEnterMarkColumn();
            studentGrid.addColumn(MarkDTO::getTotalMarkByFirstAndSecondModule)
                    .setHeader("Сума за модулі").setAutoWidth(true);
            studentGrid.addColumn(MarkDTO::getNationalGrade)
                    .setHeader("Оцінка за національною шкалою").setAutoWidth(true);
            studentGrid.addColumn(MarkDTO::getECTSGrade)
                    .setHeader("Оцінка ECTS").setAutoWidth(true);
        }
        if (typeControl.equals("Розрахункова робота") || typeControl.equals("Розрахунково-графічна робота")) {
            if (part >= 2) {
                setPart1();
                setPart2();
            }
            if (part >= 4) {
                setPart3();
                setPart4();
            }
            if (part >= 6) {
                setPart5();
                setPart6();
            }
            if (part == 8) {
                setPart7();
                setPart8();
            }
            studentGrid.addColumn(MarkDTO::getTotalGrade)
                    .setHeader("Оцінка").setAutoWidth(true);
        }
        studentGrid.addComponentColumn(markDTO -> {
            Span icon = new Span(markDTO.isLocked() ? "+" : "−");
            icon.getStyle()
                    .set("color", markDTO.isLocked() ? "green" : "red")
                    .set("font-size", "20px")
                    .set("font-weight", "bold")
                    .set("opacity", "0.7");
            return icon;
        }).setHeader("Чи заблоковано").setAutoWidth(true);
        studentGrid.addColumn(MarkDTO::getLastUpdated).setHeader("Час зміни").setAutoWidth(true);
        studentGrid.addColumn(MarkDTO::getLastUpdatedBy).setHeader("Користувач").setAutoWidth(true);
        studentGrid.setSizeFull();
        studentGrid.setWidth("100%");
    }

    private void setEnterMarkColumn() {
        studentGrid.addComponentColumn(markDTO -> {
            NumberField numberField = new NumberField();
            numberField.setMaxWidth("44px");
            numberField.getElement().getStyle().set("text-align", "center");
            if (markDTO.getEnterMark() != null && !markDTO.getEnterMark().isEmpty()) {
                numberField.setValue(Double.valueOf(markDTO.getEnterMark()));
            } else {
                numberField.setValue(null);
            }
            numberField.addValueChangeListener(event -> {
                if (event.getValue() != null) {
                    markDTO.setEnterMark(String.valueOf(event.getValue().intValue()));
                } else {
                    markDTO.setEnterMark("");
                }
            });
            return numberField;
        }).setHeader("Оцінка").setFlexGrow(1).setWidth("70px");
    }

    private void setPart1() {
        studentGrid.addComponentColumn(markDTO -> createPartNumberField(markDTO.getPartMark1(), markDTO::setPartMark1))
                .setHeader("Ч 1").setFlexGrow(1).setWidth("70px");
    }
    private void setPart2() {
        studentGrid.addComponentColumn(markDTO -> createPartNumberField(markDTO.getPartMark2(), markDTO::setPartMark2))
                .setHeader("Ч 2").setFlexGrow(1).setWidth("70px");
    }
    private void setPart3() {
        studentGrid.addComponentColumn(markDTO -> createPartNumberField(markDTO.getPartMark3(), markDTO::setPartMark3))
                .setHeader("Ч 3").setFlexGrow(1).setWidth("70px");
    }
    private void setPart4() {
        studentGrid.addComponentColumn(markDTO -> createPartNumberField(markDTO.getPartMark4(), markDTO::setPartMark4))
                .setHeader("Ч 4").setFlexGrow(1).setWidth("70px");
    }
    private void setPart5() {
        studentGrid.addComponentColumn(markDTO -> createPartNumberField(markDTO.getPartMark5(), markDTO::setPartMark5))
                .setHeader("Ч 5").setFlexGrow(1).setWidth("70px");
    }
    private void setPart6() {
        studentGrid.addComponentColumn(markDTO -> createPartNumberField(markDTO.getPartMark6(), markDTO::setPartMark6))
                .setHeader("Ч 6").setFlexGrow(1).setWidth("70px");
    }
    private void setPart7() {
        studentGrid.addComponentColumn(markDTO -> createPartNumberField(markDTO.getPartMark7(), markDTO::setPartMark7))
                .setHeader("Ч 7").setFlexGrow(1).setWidth("70px");
    }
    private void setPart8() {
        studentGrid.addComponentColumn(markDTO -> createPartNumberField(markDTO.getPartMark8(), markDTO::setPartMark8))
                .setHeader("Ч 8").setFlexGrow(1).setWidth("70px");
    }

    private NumberField createPartNumberField(String initialValue, java.util.function.Consumer<String> valueConsumer) {
        NumberField numberField = new NumberField();
        numberField.setMaxWidth("52px");
        if (initialValue != null && !initialValue.isEmpty()) {
            numberField.setValue(Double.valueOf(initialValue));
        } else {
            numberField.setValue(null);
        }
        numberField.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                valueConsumer.accept(String.valueOf(event.getValue().intValue()));
            } else {
                valueConsumer.accept("");
            }
        });
        return numberField;
    }

    private void clearGrid() {
        studentGrid.removeAllColumns();
    }

    private void setLocked(MarksEntity marksEntity) {
        marksEntity.setLocked(true);
        marksService.saveMark(marksEntity);
    }

    private void updateGrid() {
        if (selectControlType.getValue() == null) {
            return;
        }

        List<MarksEntity> marksEntityList = marksService.findMarksByPlanAndTypeControl(plansEntity, selectControlType.getValue());
        List<MarkDTO> markDTOList = new ArrayList<>();
        configureGrid(selectControlType.getValue(), plansEntity.getParts());

        // Якщо є збережені записи MarksEntity
        if (marksEntityList != null && !marksEntityList.isEmpty()) {
            // Гілка для розрахункових робіт (РР/РГР)
            if (selectControlType.getValue().equals("Розрахункова робота") ||
                    selectControlType.getValue().equals("Розрахунково-графічна робота")) {

                for (MarksEntity mark : marksEntityList) {
                    MarkDTO dto = new MarkDTO();
                    dto.setId(mark.getId());
                    dto.setStudentPIB(mark.getStudent().getFullName());

                    int totalParts = plansEntity.getParts();
                    for (int i = 1; i <= totalParts; i++) {
                        ControlPartsEntity cp = controlPartsService.getControlPartByControlMethodAndPartNumber(mark.getControlMethod(), i);
                        String partGrade = "0"; // за замовчуванням "0"
                        if (cp != null) {
                            MarksPartsEntity mpe = marksPartsService.getMarksPartByMarkAndPart(mark, cp);
                            if (mpe != null && mpe.getGrade() != null) {
                                partGrade = mpe.getGrade().toString();
                            }
                        }
                        switch (i) {
                            case 1: dto.setPartMark1(partGrade); break;
                            case 2: dto.setPartMark2(partGrade); break;
                            case 3: dto.setPartMark3(partGrade); break;
                            case 4: dto.setPartMark4(partGrade); break;
                            case 5: dto.setPartMark5(partGrade); break;
                            case 6: dto.setPartMark6(partGrade); break;
                            case 7: dto.setPartMark7(partGrade); break;
                            case 8: dto.setPartMark8(partGrade); break;
                        }
                    }
                    dto.setTotalGrade(String.valueOf(mark.getFinalGrade()));
                    dto.setLocked(mark.isLocked());
                    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                    dto.setLastUpdated(formatter.format(mark.getLastUpdated()));
                    dto.setLastUpdatedBy(mark.getLastUpdatedBy().getLastname() + " " +
                            mark.getLastUpdatedBy().getFirstname() + " " +
                            mark.getLastUpdatedBy().getPatronymic());
                    markDTOList.add(dto);
                }
            }
            // Гілка для типів "Залік", "Екзамен", "Курсова робота", "Курсовий проєкт", "Другий модульний контроль"
            else if (selectControlType.getValue().equals("Залік") ||
                    selectControlType.getValue().equals("Екзамен") ||
                    selectControlType.getValue().equals("Курсова робота") ||
                    selectControlType.getValue().equals("Курсовий проєкт") ||
                    selectControlType.getValue().equals("Другий модульний контроль")) {

                for (MarksEntity mark : marksEntityList) {
                    MarkDTO dto = new MarkDTO();
                    dto.setId(mark.getId());
                    dto.setStudentPIB(mark.getStudent().getFullName());

                    // Використовуємо finalGrade для конвертації
                    int finalGrade = mark.getFinalGrade();
                    dto.setEnterMark(String.valueOf(finalGrade));
                    dto.setNationalGrade(convertMarkToNationalGrade(finalGrade));
                    dto.setECTSGrade(convertMarkToECTSGrade(finalGrade));

                    // Отримуємо оцінки для першого і другого модулів
                    String firstModule = marksService.getMarkForFirstModalControl(mark.getStudent(), plansEntity, "Перший модульний контроль");
                    if (firstModule == null || firstModule.isEmpty()) {
                        firstModule = "0";
                    }
                    String secondModule = marksService.getMarkForFirstModalControl(mark.getStudent(), plansEntity, "Другий модульний контроль");
                    if (secondModule == null || secondModule.isEmpty()) {
                        secondModule = "0"; // якщо немає другого, підставляємо перший
                    }
                    dto.setMarkByFirstModule(firstModule);
                    int sumModules = Integer.parseInt(firstModule) + Integer.parseInt(secondModule);
                    dto.setTotalMarkByFirstAndSecondModule(String.valueOf(sumModules));

                    dto.setLocked(mark.isLocked());
                    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                    dto.setLastUpdated(formatter.format(mark.getLastUpdated()));
                    dto.setLastUpdatedBy(mark.getLastUpdatedBy().getLastname() + " " +
                            mark.getLastUpdatedBy().getFirstname() + " " +
                            mark.getLastUpdatedBy().getPatronymic());
                    markDTOList.add(dto);
                }
            }
            // Фолбек для інших типів контролю
            else {
                for (MarksEntity mark : marksEntityList) {
                    MarkDTO dto = new MarkDTO();
                    dto.setId(mark.getId());
                    dto.setStudentPIB(mark.getStudent().getFullName());
                    dto.setEnterMark(String.valueOf(mark.getFinalGrade()));
                    dto.setLocked(mark.isLocked());
                    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                    dto.setLastUpdated(formatter.format(mark.getLastUpdated()));
                    dto.setLastUpdatedBy(mark.getLastUpdatedBy().getLastname() + " " +
                            mark.getLastUpdatedBy().getFirstname() + " " +
                            mark.getLastUpdatedBy().getPatronymic());
                    markDTOList.add(dto);
                }
            }
            studentGrid.setItems(markDTOList);
        }
        // Якщо немає жодного MarksEntity, завантажуємо студентів із групи та намагаємося підвантажити модульні оцінки
        else {
            StudentGroupEntity studentGroupEntity = plansEntity.getGroup();
            List<StudentEntity> studentEntities;
            if (plansEntity.isElective()) {
                studentEntities = studentPlansService.getStudentByPlan(plansEntity);
            } else {
                studentEntities = studentService.getStudentByGroupId(studentGroupEntity.getId());
            }
            List<MarkDTO> fallbackList = new ArrayList<>();
            long id = 1;
            // Перевіряємо, чи тип контролю вказує на модульну логіку
            boolean useModuleData = (selectControlType.getValue().equals("Залік") ||
                    selectControlType.getValue().equals("Екзамен") ||
                    selectControlType.getValue().equals("Курсова робота") ||
                    selectControlType.getValue().equals("Курсовий проєкт") ||
                    selectControlType.getValue().equals("Другий модульний контроль"));

            for (StudentEntity student : studentEntities) {
                MarkDTO dto = new MarkDTO();
                dto.setId(id);
                dto.setStudentPIB(student.getSurname() + " " + student.getName() + " " + student.getPatronymic());
                if (useModuleData) {
                    try {
                        StudentEntity stud = studentService.findStudentById(student.getId());
                        String firstModule = marksService.getMarkForFirstModalControl(stud, plansEntity, "Перший модульний контроль");
                        if (firstModule == null || firstModule.isEmpty()) {
                            firstModule = "0";
                        }
                        String secondModule = marksService.getMarkForFirstModalControl(stud, plansEntity, "Другий модульний контроль");
                        if (secondModule == null || secondModule.isEmpty()) {
                            secondModule = "0";
                        }
                        dto.setMarkByFirstModule(firstModule);
                        int sumModules = Integer.parseInt(firstModule) + Integer.parseInt(secondModule);
                        dto.setTotalMarkByFirstAndSecondModule(String.valueOf(sumModules));
                        // Так само, для перетворення finalGrade використаємо суму модулів
                        dto.setNationalGrade(convertMarkToNationalGrade(sumModules));
                        dto.setECTSGrade(convertMarkToECTSGrade(sumModules));
                    } catch (Exception e) {
                        dto.setMarkByFirstModule("0");
                        dto.setTotalMarkByFirstAndSecondModule("0");
                        dto.setEnterMark("0");
                        dto.setNationalGrade(convertMarkToNationalGrade(0));
                        dto.setECTSGrade(convertMarkToECTSGrade(0));
                    }
                }
                dto.setLocked(false);
                dto.setLastUpdated("");
                dto.setLastUpdatedBy("");
                fallbackList.add(dto);
                id++;
            }
            studentGrid.setItems(fallbackList);
        }
    }









    private String convertMarkToNationalGrade(int mark) {
        if (mark >= 90) {
            return "Відмінно";
        } else if (mark >= 82) {
            return "Добре";
        } else if (mark >= 74) {
            return "Добре";
        } else if (mark >= 64) {
            return "Задовільно";
        } else if (mark >= 60) {
            return "Задовільно";
        } else if (mark >= 35) {
            return "Незадовільно";
        } else {
            return "Незадовільно";
        }
    }

    private String convertMarkToECTSGrade(int mark) {
        if (mark >= 90) {
            return "A";
        } else if (mark >= 82) {
            return "B";
        } else if (mark >= 74) {
            return "C";
        } else if (mark >= 64) {
            return "D";
        } else if (mark >= 60) {
            return "E";
        } else if (mark >= 35) {
            return "FX";
        } else {
            return "F";
        }
    }
}
