package com.esvar.dekanat.mark;

import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.dto.MarkDTO;
import com.esvar.dekanat.entity.*;
import com.esvar.dekanat.security.SecurityService;
import com.esvar.dekanat.service.*;
import com.esvar.dekanat.user.UserModel;
import com.esvar.dekanat.user.UserRepository;
import com.esvar.dekanat.view.MainLayout;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.sql.Timestamp;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@PageTitle("Введення оцінок | Деканат")
@Route(value = "marks", layout = MainLayout.class)
@PermitAll
public class EnterMarksView extends Div {

    private static final Logger log = LoggerFactory.getLogger(EnterMarksView.class);
    private static final String CONTROL_TYPE_FIRST_MODULE = "Перший модульний контроль";
    private static final String CONTROL_TYPE_SECOND_MODULE = "Другий модульний контроль";
    private static final String CONTROL_TYPE_CONTROL_WORK = "Контрольна робота";
    private static final String SYSTEM_USER_DISPLAY_NAME = "Система";
    private static final String DATE_TIME_PATTERN = "dd.MM.yyyy HH:mm";

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
    private final GroupService groupService;


    private VerticalLayout mainLayout = new VerticalLayout();
    private HorizontalLayout contentLayout = new HorizontalLayout();
    private VerticalLayout leftLayout = new VerticalLayout();
    private VerticalLayout rightLayout = new VerticalLayout();
    private HorizontalLayout buttonLayout = new HorizontalLayout();

    private Select<String> selectFaculty = new Select<>();
    private Select<String> selectDepartment = new Select<>();
    private Select<String> selectSpecialty = new Select<>();
    private Select<String> selectCourse = new Select<>();
    private final Select<GroupDTO> selectGroup = new Select<>();
    private Select<String> selectDiscipline = new Select<>();
    private Select<String> selectControlType = new Select<>();
    private PlansEntity plansEntity = new PlansEntity();
    private StudentGroupEntity currentGroup;
    private Grid<MarkDTO> studentGrid = new Grid<>(MarkDTO.class, false);

    private final Collator ukrainianCollator = Collator.getInstance(new Locale("uk", "UA"));

    public EnterMarksView(FacultyService facultyService, DepartmentService departmentService, PlanService planService,
                          StudentService studentService, StudentPlansService studentPlansService, SecurityService securityService,
                          UserRepository userRepository, MarksService marksService, ControlMethodService controlMethodService,
                          MarksPartsService marksPartsService, ControlPartsService controlPartsService, GroupService groupService) {
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
        this.groupService = groupService;

        // Налаштування форми вибору параметрів
        selectFaculty.setLabel("Факультет");
        selectFaculty.setWidth("100%");
        selectFaculty.getStyle().set("padding", "0px").set("margin", "0px").set("margin-bottom", "5px");
        selectFaculty.setItems(facultyService.getFacultyTitles());

        selectDepartment.setLabel("Кафедра");
        selectDepartment.setWidth("100%");
        selectDepartment.getStyle().set("padding", "0px").set("margin", "0px").set("margin-bottom", "5px");
        selectDepartment.setItems(departmentService.getAllDepartment());

        UserDetails u = securityService.getAuthenticatedUser();
        if (u == null) return;  // не залогінені → VaadinWebSecurity на /login

        Set<String> roles = u.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

        String role_type = securityService.getCurrentRoleType();
        boolean isDepartment   = roles.stream().anyMatch(r -> r.startsWith("ROLE_DEPARTMENT"));
        boolean isDekanatGroup = roles.stream().anyMatch(r -> r.startsWith("ROLE_DEKANAT"));
        boolean isAdmin        = roles.stream().anyMatch(r -> r.startsWith("ROLE_ADMIN"));

        if (isDekanatGroup){
            selectFaculty.setValue(facultyService.getFacultyTitleById(Long.valueOf(role_type)));
        }

        selectSpecialty.setLabel("Спеціальність");
        selectSpecialty.setWidth("100%");
        selectSpecialty.getStyle().set("padding", "0px").set("margin", "0px").set("margin-bottom", "5px");

        selectCourse.setLabel("Курс");
        selectCourse.setWidth("100%");
        selectCourse.getStyle().set("padding", "0px").set("margin", "0px").set("margin-bottom", "5px");

        selectGroup.setLabel("Група");
        selectGroup.setWidth("100%");
        selectGroup.getStyle().set("padding", "0px").set("margin", "0px").set("margin-bottom", "5px");
        selectGroup.setItemLabelGenerator(GroupDTO::getGroupCode);

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
        unlockButton.setVisible(isAdmin || isDekanatGroup);

        buttonLayout.add(saveButton, approveButton, unlockButton);
        buttonLayout.setWidth("100%");
        buttonLayout.setFlexGrow(1, saveButton, approveButton, unlockButton);
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

        if (isDepartment) {
            selectDepartment.setItems(departmentService.getDepartmentById(Long.valueOf(role_type)));
            selectDepartment.setValue(departmentService.getDepartmentById(Long.valueOf(role_type)));
            selectDepartment.setVisible(false);
        }
        if (isDekanatGroup) {
            selectDepartment.setReadOnly(false);
            selectFaculty.setReadOnly(true);
            selectFaculty.setVisible(false);
        }

        // Обробники подій для селектів
        selectFaculty.addValueChangeListener(event -> {
            clearGrid();


            selectDepartment.setReadOnly(isDepartment);
            if (isDepartment) selectSpecialty.setItems(planService.getSpecialtiesByFacultyAndDepartment(selectFaculty.getValue(), selectDepartment.getValue()));
            else selectDepartment.setItems(departmentService.getAllDepartment());
            selectSpecialty.setReadOnly(!isDepartment);
            selectCourse.setReadOnly(true);
            selectGroup.setReadOnly(true);
            selectDiscipline.setReadOnly(true);
            selectControlType.setReadOnly(true);

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
            currentGroup = null;
            if (selectCourse.getValue() != null) {
                selectGroup.setReadOnly(false);
                selectDiscipline.setReadOnly(true);
                selectControlType.setReadOnly(true);

                selectDiscipline.clear();
                selectControlType.clear();

                selectGroup.setItems(planService.getGroupsByFacultyAndDepartmentAndSpecialtyAndCourse(
                        selectFaculty.getValue(),
                        selectDepartment.getValue(),
                        selectSpecialty.getValue(),
                        Integer.parseInt(selectCourse.getValue())
                ));
            }
        });

        selectGroup.addValueChangeListener(event -> {
            clearGrid();
            GroupDTO selectedGroup = selectGroup.getValue();
            currentGroup = selectedGroup == null ? null : groupService.getGroupByTitle(selectedGroup.getGroupCode());
            if (selectedGroup != null) {
                selectDiscipline.setReadOnly(false);
                selectControlType.setReadOnly(true);

                selectControlType.clear();

                selectDiscipline.setItems(planService.getDisciplinesByFacultyAndDepartmentAndSpecialtyAndGroupCourseAndGroupGroupNumber(
                        selectFaculty.getValue(),
                        selectDepartment.getValue(),
                        selectSpecialty.getValue(),
                        selectedGroup.getCourse(),
                        selectedGroup.getGroupNumber()
                ));
            }
        });

        selectDiscipline.addValueChangeListener(event -> {
            clearGrid();
            GroupDTO selectedGroup = selectGroup.getValue();
            if (selectDiscipline.getValue() != null && selectedGroup != null) {
                selectControlType.setReadOnly(false);

                selectControlType.setItems(planService.getControlTypesByFacultyAndDepartmentAndSpecialtyAndGroupCourseAndGroupNumberAndDiscipline(
                        selectFaculty.getValue(),
                        selectDepartment.getValue(),
                        selectSpecialty.getValue(),
                        selectedGroup.getCourse(),
                        selectedGroup.getGroupNumber(),
                        selectDiscipline.getValue()
                ));

                plansEntity = planService.getPlanEntityByFacultyAndDepartmentAndSpecialtyAndGroupCourseAndGroupNumberAndDiscipline(
                        selectFaculty.getValue(),
                        selectDepartment.getValue(),
                        selectSpecialty.getValue(),
                        selectedGroup.getCourse(),
                        selectedGroup.getGroupNumber(),
                        selectDiscipline.getValue()
                );
            }
        });

        selectControlType.addValueChangeListener(event -> updateGrid());

        // Обробник кнопки "Зберегти" із використанням фабрики процесорів
        saveButton.addClickListener(event -> {
            List<MarkDTO> markDTOList = getSelectedOrAllMarks();

            String controlType = selectControlType.getValue();
            MarkProcessor processor = MarkProcessorFactory.getProcessor(controlType, marksService, userRepository,
                    securityService, studentService, marksPartsService, controlMethodService, controlPartsService);

            List<MarksEntity> toSave = new ArrayList<>();

            for (MarkDTO markDTO : markDTOList) {
                if (markDTO.isLocked()) {
                    continue;
                }
                MarksEntity marksEntity = processor.processMark(markDTO, plansEntity, requireCurrentGroup(), controlType);
                if (!processor.isPersistedAfterProcessing()) {
                    toSave.add(marksEntity);
                }
            }
            if (!processor.isPersistedAfterProcessing()) {
                marksService.saveMarks(toSave);
            }
            updateGrid();
        });

        // Обробники кнопок "Затвердити" та "Розблокувати"
        approveButton.addClickListener(event -> {
            List<MarkDTO> markDTOList = getSelectedOrAllMarks();
            String controlType = selectControlType.getValue();
            MarkProcessor processor = MarkProcessorFactory.getProcessor(controlType, marksService, userRepository,
                    securityService, studentService, marksPartsService, controlMethodService, controlPartsService);
            List<MarksEntity> toSave = new ArrayList<>();
            for (MarkDTO markDTO : markDTOList) {
                if (markDTO.isLocked()) {
                    continue;
                }
                MarksEntity marksEntity = processor.processMark(markDTO, plansEntity, requireCurrentGroup(), controlType);
                marksEntity.setLastUpdated(new Timestamp(System.currentTimeMillis()));
                marksEntity.setLastUpdatedBy(userRepository.findByEmail(securityService.getAuthenticatedUser().getUsername()).orElseThrow());
                marksEntity.setLocked(true);
                toSave.add(marksEntity);
            }
            marksService.saveMarks(toSave);
            updateGrid();
        });

        unlockButton.addClickListener(event -> {
            List<MarkDTO> markDTOList = getSelectedOrAllMarks();
            List<MarksEntity> toSave = new ArrayList<>();
            for (MarkDTO markDTO : markDTOList) {
                if (!markDTO.isLocked()) {
                    continue;
                }
                MarksEntity marksEntity = marksService.getMarkById(markDTO.getId());
                marksEntity.setLastUpdated(new Timestamp(System.currentTimeMillis()));
                marksEntity.setLastUpdatedBy(userRepository.findByEmail(securityService.getAuthenticatedUser().getUsername()).orElseThrow());
                marksEntity.setLocked(false);
                toSave.add(marksEntity);
            }
            marksService.saveMarks(toSave);
            updateGrid();
        });

    }

    private void configureGrid(String typeControl, int part) {
        studentGrid.removeAllColumns();
        studentGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        studentGrid.addColumn(MarkDTO::getRowNum)
                .setHeader("№")
                .setFlexGrow(1).setWidth("35px");
        studentGrid.addColumn(MarkDTO::getStudentPIB)
                .setHeader("ПІБ студента")
                .setFlexGrow(3).setWidth("250px");

        if (CONTROL_TYPE_FIRST_MODULE.equals(typeControl)) {
            setEnterMarkColumn(false);
        }
        if (CONTROL_TYPE_CONTROL_WORK.equals(typeControl)) {
            setEnterMarkColumn(true);
            addControlWorkAdmissionColumn();
        }
        if (CONTROL_TYPE_SECOND_MODULE.equals(typeControl)) {
            studentGrid.addColumn(MarkDTO::getMarkByFirstModule)
                    .setHeader("Перший модуль").setAutoWidth(true);
            setEnterMarkColumn(false);
            studentGrid.addColumn(MarkDTO::getTotalMarkByFirstAndSecondModule)
                    .setHeader(new Html("<div style='white-space:normal; word-break:break-word; text-align:center; line-height:1.2; max-width:50px;'>Cума<br>за<br>модулі</div>")).setAutoWidth(false);
        }
        if (typeControl.equals("Залік") ||
                typeControl.equals("Екзамен") ||
                typeControl.equals("Курсова робота") ||
                typeControl.equals("Курсовий проєкт") ||
                typeControl.equals("Диференційний залік")) {
            setEnterMarkColumn(false);
            studentGrid.addColumn(MarkDTO::getTotalMarkByFirstAndSecondModule)
                    .setHeader(new Html("<div style='white-space:normal; word-break:break-word; text-align:center; line-height:1.2; max-width:50px;'>Cума<br>за<br>модулі</div>")).setAutoWidth(false);
            studentGrid.addColumn(MarkDTO::getNationalGrade)
                    .setHeader(new Html("<div style='white-space:normal; word-break:break-word; text-align:center; line-height:1.2; max-width:95px;'>Оцінка за<br>національною шкалою</div>")).setAutoWidth(false);
            studentGrid.addColumn(MarkDTO::getECTSGrade)
                    .setHeader(new Html("<div style='white-space:normal; word-break:break-word; text-align:center; line-height:1.2; max-width:60px;'>Оцінка<br>ECTS</div>")).setAutoWidth(false);
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
        }).setHeader("Заблоковано").setAutoWidth(true);
        studentGrid.addColumn(MarkDTO::getLastUpdated).setHeader("Час зміни").setAutoWidth(true);
        studentGrid.addColumn(MarkDTO::getLastUpdatedBy).setHeader("Користувач").setAutoWidth(true);
        studentGrid.setSizeFull();
        studentGrid.setWidth("100%");
    }

    private void setEnterMarkColumn(boolean controlWorkMode) {
        studentGrid.addComponentColumn(markDTO -> {
            IntegerField integerField = new IntegerField();
            integerField.setMaxWidth("44px");
            integerField.getElement().getStyle().set("text-align", "center");
            if (markDTO.getEnterMark() != null && !markDTO.getEnterMark().isEmpty()) {
                integerField.setValue(Integer.valueOf(markDTO.getEnterMark()));
            } else {
                integerField.clear();
            }
            // Встановлюємо readOnly в залежності від прапорця locked
            integerField.setReadOnly(markDTO.isLocked());
            integerField.addValueChangeListener(event -> {
                if (!markDTO.isLocked()) {
                    if (event.getValue() != null) {
                        markDTO.setEnterMark(String.valueOf(event.getValue()));
                    } else {
                        markDTO.setEnterMark("");
                    }
                    if (controlWorkMode) {
                        markDTO.setControlWorkAdmission(calculateControlWorkAdmission(markDTO.getEnterMark()));
                        try {
                            studentGrid.getListDataView().refreshItem(markDTO);
                        } catch (IllegalStateException ignored) {
                        }
                    }
                }
            });
            return integerField;
        }).setHeader("Оцінка").setFlexGrow(1).setWidth("80px");
    }

    private void addControlWorkAdmissionColumn() {
        studentGrid.addColumn(markDTO -> {
                    String status = markDTO.getControlWorkAdmission();
                    return status == null ? "" : status;
                })
                .setHeader(new Html("<div style='white-space:normal; word-break:break-word; text-align:center; line-height:1.2; max-width:120px;'>Відмітка<br>про зарахування<br>контрольної роботи</div>"))
                .setAutoWidth(false);
    }

    private void setPart1() {
        studentGrid.addComponentColumn(markDTO ->
                createPartNumberField(markDTO.getPartMark1(), markDTO::setPartMark1, markDTO.isLocked())
        ).setHeader("Ч 1").setFlexGrow(1).setWidth("70px");
    }
    private void setPart2() {
        studentGrid.addComponentColumn(markDTO ->
                createPartNumberField(markDTO.getPartMark2(), markDTO::setPartMark2, markDTO.isLocked())
        ).setHeader("Ч 2").setFlexGrow(1).setWidth("70px");
    }
    private void setPart3() {
        studentGrid.addComponentColumn(markDTO ->
                createPartNumberField(markDTO.getPartMark3(), markDTO::setPartMark3, markDTO.isLocked())
        ).setHeader("Ч 3").setFlexGrow(1).setWidth("70px");
    }
    private void setPart4() {
        studentGrid.addComponentColumn(markDTO ->
                createPartNumberField(markDTO.getPartMark4(), markDTO::setPartMark4, markDTO.isLocked())
        ).setHeader("Ч 4").setFlexGrow(1).setWidth("70px");
    }
    private void setPart5() {
        studentGrid.addComponentColumn(markDTO ->
                createPartNumberField(markDTO.getPartMark5(), markDTO::setPartMark5, markDTO.isLocked())
        ).setHeader("Ч 5").setFlexGrow(1).setWidth("70px");
    }
    private void setPart6() {
        studentGrid.addComponentColumn(markDTO ->
                createPartNumberField(markDTO.getPartMark6(), markDTO::setPartMark6, markDTO.isLocked())
        ).setHeader("Ч 6").setFlexGrow(1).setWidth("70px");
    }
    private void setPart7() {
        studentGrid.addComponentColumn(markDTO ->
                createPartNumberField(markDTO.getPartMark7(), markDTO::setPartMark7, markDTO.isLocked())
        ).setHeader("Ч 7").setFlexGrow(1).setWidth("70px");
    }
    private void setPart8() {
        studentGrid.addComponentColumn(markDTO ->
                createPartNumberField(markDTO.getPartMark8(), markDTO::setPartMark8, markDTO.isLocked())
        ).setHeader("Ч 8").setFlexGrow(1).setWidth("70px");
    }

    private IntegerField createPartNumberField(String initialValue, java.util.function.Consumer<String> valueConsumer, boolean locked) {
        IntegerField integerField = new IntegerField();
        integerField.setMaxWidth("52px");
        integerField.getElement().getStyle().set("text-align", "center");
        if (initialValue != null && !initialValue.isEmpty()) {
            integerField.setValue(Integer.valueOf(initialValue));
        } else {
            integerField.clear();
        }
        // Встановлюємо режим readOnly в залежності від прапорця locked
        integerField.setReadOnly(locked);
        integerField.addValueChangeListener(event -> {
            if (!locked) {
                if (event.getValue() != null) {
                    valueConsumer.accept(String.valueOf(event.getValue()));
                } else {
                    valueConsumer.accept("");
                }
            }
        });
        return integerField;
    }

    private void clearGrid() {
        studentGrid.removeAllColumns();
        studentGrid.setSelectionMode(Grid.SelectionMode.NONE);
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
            List<MarksEntity> sortedMarks = marksEntityList.stream()
                    .sorted(Comparator.comparing(mark -> mark.getStudent().getFullName(), ukrainianCollator))
                    .collect(Collectors.toList());
            if (selectControlType.getValue().equals("Розрахункова робота") ||
                    selectControlType.getValue().equals("Розрахунково-графічна робота")) {

                for (MarksEntity mark : sortedMarks) {
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
                    populateAuditInfo(dto, mark);
                    markDTOList.add(dto);
                }
            }

            else if (CONTROL_TYPE_CONTROL_WORK.equals(selectControlType.getValue())) {
                for (MarksEntity mark : sortedMarks) {
                    MarkDTO dto = new MarkDTO();
                    dto.setId(mark.getId());
                    dto.setStudentPIB(mark.getStudent().getFullName());

                    int finalGrade = mark.getFinalGrade();
                    dto.setEnterMark(String.valueOf(finalGrade));
                    dto.setControlWorkAdmission(calculateControlWorkAdmission(dto.getEnterMark()));
                    dto.setLocked(mark.isLocked());
                    populateAuditInfo(dto, mark);
                    markDTOList.add(dto);
                }
            }
            // Гілка для типів "Залік", "Екзамен", "Курсова робота", "Курсовий проєкт", "Другий модульний контроль"
            else if (selectControlType.getValue().equals("Залік") ||
                    selectControlType.getValue().equals("Екзамен") ||
                    selectControlType.getValue().equals("Курсова робота") ||
                    selectControlType.getValue().equals("Курсовий проєкт") ||
                    selectControlType.getValue().equals("Диференційний залік")  ||
                    CONTROL_TYPE_SECOND_MODULE.equals(selectControlType.getValue())) {

                for (MarksEntity mark : sortedMarks) {
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
                    populateAuditInfo(dto, mark);
                    markDTOList.add(dto);
                }
            }
            // Фолбек для інших типів контролю
            else {
                for (MarksEntity mark : sortedMarks) {
                    MarkDTO dto = new MarkDTO();
                    dto.setId(mark.getId());
                    dto.setStudentPIB(mark.getStudent().getFullName());
                    dto.setEnterMark(String.valueOf(mark.getFinalGrade()));
                    dto.setLocked(mark.isLocked());
                    populateAuditInfo(dto, mark);
                    markDTOList.add(dto);


                }
            }
            ensureAllStudentsPresent(markDTOList);
            setRowNumbers(markDTOList);
            studentGrid.setItems(markDTOList);
        }
        // Якщо немає жодного MarksEntity, завантажуємо студентів із групи та намагаємося підвантажити модульні оцінки
        else {
            StudentGroupEntity studentGroupEntity = getPlanContextGroup();
            List<StudentEntity> studentEntities = getSortedStudentsForPlan(studentGroupEntity);
            List<MarkDTO> fallbackList = new ArrayList<>();
            long id = 1;
            // Перевіряємо, чи тип контролю вказує на модульну логіку

            for (StudentEntity student : studentEntities) {
                MarkDTO dto = createPlaceholderMarkDTO(student);
                dto.setId(id);

                fallbackList.add(dto);
                id++;
            }
            setRowNumbers(fallbackList);
            studentGrid.setItems(fallbackList);
        }
    }

    /**
     * Проставляє порядкові номери для переданої колекції записів.
     */
    private void setRowNumbers(List<MarkDTO> list) {
        int i = 1;
        for (MarkDTO dto : list) {
            dto.setRowNum(i++);
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


    private void ensureAllStudentsPresent(List<MarkDTO> markDTOList) {
        StudentGroupEntity studentGroupEntity = getPlanContextGroup();
        List<StudentEntity> studentEntities = getSortedStudentsForPlan(studentGroupEntity);
        Set<String> existingStudents = markDTOList.stream()
                .map(MarkDTO::getStudentPIB)
                .collect(Collectors.toSet());

        for (StudentEntity student : studentEntities) {
            String fullName = student.getFullName();
            if (!existingStudents.contains(fullName)) {
                markDTOList.add(createPlaceholderMarkDTO(student));
            }
        }

        markDTOList.sort(Comparator.comparing(MarkDTO::getStudentPIB, ukrainianCollator));
    }

    private StudentGroupEntity getPlanContextGroup() {
        if (currentGroup != null) {
            return currentGroup;
        }
        if (plansEntity == null) {
            return null;
        }
        if (plansEntity.getGroups() != null && !plansEntity.getGroups().isEmpty()) {
            return plansEntity.getGroups().iterator().next();
        }
        return plansEntity.getGroup();
    }

    private StudentGroupEntity requireCurrentGroup() {
        StudentGroupEntity group = getPlanContextGroup();
        if (group == null) {
            throw new IllegalStateException("Групу для поточного плану не обрано.");
        }
        return group;
    }

    private MarkDTO createPlaceholderMarkDTO(StudentEntity student) {
        MarkDTO dto = new MarkDTO();
        dto.setStudentPIB(student.getFullName());
        dto.setEnterMark("");
        dto.setLocked(false);
        dto.setLastUpdated("");
        dto.setLastUpdatedBy("");
        populateModuleAggregatesIfNeeded(dto, student);
        dto.setControlWorkAdmission(calculateControlWorkAdmission(dto.getEnterMark()));
        return dto;
    }

    private void populateModuleAggregatesIfNeeded(MarkDTO dto, StudentEntity student) {
        String controlType = selectControlType.getValue();
        if (controlType == null) {
            return;
        }
        boolean useModuleData = controlType.equals("Залік") ||
                controlType.equals("Екзамен") ||
                controlType.equals("Курсова робота") ||
                controlType.equals("Курсовий проєкт") ||
                controlType.equals("Диференційний залік") ||
                controlType.equals("Другий модульний контроль") ||
                CONTROL_TYPE_SECOND_MODULE.equals(controlType);
        if (!useModuleData) {
            return;
        }
        try {
            StudentEntity stud = studentService.findStudentById(student.getId());
            ModuleData moduleData = resolveModuleDataForStudent(stud);
            dto.setMarkByFirstModule(moduleData.firstModule());
            dto.setTotalMarkByFirstAndSecondModule(String.valueOf(moduleData.total()));
            dto.setNationalGrade(convertMarkToNationalGrade(moduleData.total()));
            dto.setECTSGrade(convertMarkToECTSGrade(moduleData.total()));
        } catch (Exception e) {
            dto.setMarkByFirstModule("0");
            dto.setTotalMarkByFirstAndSecondModule("0");
            dto.setEnterMark("0");
            dto.setNationalGrade(convertMarkToNationalGrade(0));
            dto.setECTSGrade(convertMarkToECTSGrade(0));
        }
    }

    private ModuleData resolveModuleDataForStudent(StudentEntity student) {
        if (student == null) {
            return new ModuleData("0", "0", 0);
        }
        String firstModule = normalizeModuleValue(
                marksService.getMarkForFirstModalControl(student, plansEntity, CONTROL_TYPE_FIRST_MODULE));
        String secondModule = normalizeModuleValue(
                marksService.getMarkForFirstModalControl(student, plansEntity, CONTROL_TYPE_SECOND_MODULE));
        if (!student.isFullTime()) {
            String controlWork = normalizeModuleValue(
                    marksService.getMarkForFirstModalControl(student, plansEntity, CONTROL_TYPE_CONTROL_WORK));
            firstModule = controlWork;
            secondModule = "0";
        }
        int total = parseToInt(firstModule) + parseToInt(secondModule);
        return new ModuleData(firstModule, secondModule, total);
    }

    private String normalizeModuleValue(String value) {
        if (value == null || value.isBlank()) {
            return "0";
        }
        try {
            Integer.parseInt(value);
            return value;
        } catch (NumberFormatException e) {
            return "0";
        }
    }

    private int parseToInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String calculateControlWorkAdmission(String markValue) {
        if (markValue == null || markValue.isBlank()) {
            return "";
        }
        try {
            int grade = Integer.parseInt(markValue);
            return grade >= 20 ? "Зараховано" : "Не зараховано";
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private record ModuleData(String firstModule, String secondModule, int total) {
    }

    private List<StudentEntity> getSortedStudentsForPlan(StudentGroupEntity studentGroupEntity) {
        if (plansEntity.isElective()) {
            return sortStudentsByFullName(studentPlansService.getStudentByPlan(plansEntity));
        }
        StudentGroupEntity group = studentGroupEntity != null ? studentGroupEntity : getPlanContextGroup();
        if (group == null) {
            return Collections.emptyList();
        }
        return sortStudentsByFullName(studentService.getStudentByGroupId(group.getId()));
    }

    private void populateAuditInfo(MarkDTO dto, MarksEntity mark) {
        dto.setLastUpdated(formatLastUpdated(mark.getLastUpdated()));
        dto.setLastUpdatedBy(resolveLastUpdatedBy(mark));
    }

    private String resolveLastUpdatedBy(MarksEntity mark) {
        if (mark == null) {
            return "";
        }
        return formatUserName(mark.getLastUpdatedBy());
    }

    private String formatLastUpdated(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        return new SimpleDateFormat(DATE_TIME_PATTERN).format(timestamp);
    }

    private String formatUserName() {
        UserModel user = securityService.getCurrentUserModel().orElse(null);
        return formatUserName(user);
    }

    private String formatUserName(UserModel user) {
        if (user == null) {
            return SYSTEM_USER_DISPLAY_NAME;
        }
        String firstname = Optional.ofNullable(user.getFirstname()).orElse("").trim();
        String lastname = Optional.ofNullable(user.getLastname()).orElse("").trim();
        String formattedLast = lastname.isEmpty() ? "" : lastname.toUpperCase();
        String display = (firstname + " " + formattedLast).trim();
        if (!display.isEmpty()) {
            return display;
        }
        return Optional.ofNullable(user.getEmail()).orElse(SYSTEM_USER_DISPLAY_NAME);
    }

    private List<StudentEntity> sortStudentsByFullName(List<StudentEntity> students) {
        return students.stream()
                .sorted(Comparator.comparing(StudentEntity::getFullName, ukrainianCollator))
                .collect(Collectors.toList());
    }


}
