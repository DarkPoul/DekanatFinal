package com.esvar.dekanat.mark;


import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.dto.MarkDTO;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import com.esvar.dekanat.security.SecurityService;
import com.esvar.dekanat.service.*;
import com.esvar.dekanat.user.UserRepository;
import com.esvar.dekanat.view.MainLayout;
import com.vaadin.flow.component.UI;
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

//todo замінити колонки на плаваючі для економії місця

@PageTitle("Введення оцінок | Деканат")
@Route(value = "marks", layout = MainLayout.class)
@PermitAll
public class EnterMarksView extends Div {

    private final FacultyService facultyService;
    private final DepartmentService departmentService;
    private final PlanService planService;
    private final StudentService studentService;

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
    private final StudentPlansService studentPlansService;
    private final SecurityService securityService;
    private final UserRepository userRepository;
    private final MarksService marksService;

    private final ControlMethodService controlMethodService;


    public EnterMarksView(FacultyService facultyService, DepartmentService departmentService, PlanService planService, StudentService studentService, StudentPlansService studentPlansService, SecurityService securityService, UserRepository userRepository, MarksService marksService, ControlMethodService controlMethodService) {
        this.facultyService = facultyService;
        this.departmentService = departmentService;
        this.planService = planService;
        this.studentService = studentService;
        this.studentPlansService = studentPlansService;
        this.securityService = securityService;
        this.userRepository = userRepository;
        this.marksService = marksService;
        this.controlMethodService = controlMethodService;


        // Форма з вибором параметрів
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

        // Кнопки
        Button saveButton = new Button("Зберегти", new Icon(VaadinIcon.CLIPBOARD_CHECK));
        Button approveButton = new Button("Затвердити", new Icon(VaadinIcon.CHECK_CIRCLE));
        Button unlockButton = new Button("Розблокувати", new Icon(VaadinIcon.UNLOCK));
        Button printReportButton = new Button("Друк відомості", new Icon(VaadinIcon.PRINT));
        Button additionalReportButton = new Button("Додаткова відомість", new Icon(VaadinIcon.FILE_ADD));

        buttonLayout.add(saveButton, approveButton, unlockButton, printReportButton, additionalReportButton);
        buttonLayout.setWidth("100%");
        buttonLayout.setFlexGrow(1, saveButton, approveButton, unlockButton, printReportButton, additionalReportButton);
        buttonLayout.getStyle().set("gap", "10px");

        // Таблиця студентів
        studentGrid.getStyle().set("border-radius", "8px");
        studentGrid.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        studentGrid.getStyle().set("position", "relative");
        studentGrid.getStyle().set("background-color", "white");
        studentGrid.getStyle().set("padding", "16px");

        rightLayout.add(buttonLayout, studentGrid);
        rightLayout.getStyle().set("height", "calc(100vh - 80px)");
        rightLayout.setWidthFull();

        // Ліва панель з вибором параметрів
        VerticalLayout leftContainer = new VerticalLayout(leftLayout);
        leftContainer.setWidth("20%");
        leftContainer.setPadding(false);

        // Компонування лівої та правої панелей
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





        selectFaculty.addValueChangeListener(selectStringComponentValueChangeEvent -> {
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


        selectDepartment.addValueChangeListener(selectStringComponentValueChangeEvent -> {
            clearGrid();
            if (selectDepartment.getValue() != null){
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

        selectSpecialty.addValueChangeListener(selectStringComponentValueChangeEvent -> {
            clearGrid();
            if (selectSpecialty.getValue() != null){
                selectCourse.setReadOnly(false);
                selectGroup.setReadOnly(true);
                selectDiscipline.setReadOnly(true);
                selectControlType.setReadOnly(true);

                selectGroup.clear();
                selectDiscipline.clear();
                selectControlType.clear();

                selectCourse.setItems(planService.getCourseByFacultyAndDepartmentAndSpecialty
                        (
                                selectFaculty.getValue(),
                                selectDepartment.getValue(),
                                selectSpecialty.getValue()
                        )
                );
            }

        });

        selectCourse.addValueChangeListener(selectStringComponentValueChangeEvent -> {
            clearGrid();
            if (selectCourse.getValue() != null){
                selectGroup.setReadOnly(false);
                selectDiscipline.setReadOnly(true);
                selectControlType.setReadOnly(true);

                selectDiscipline.clear();
                selectControlType.clear();

                selectGroup.setItems(planService.getNumGroupsByFacultyAndDepartmentAndSpecialtyAndCourse
                        (
                                selectFaculty.getValue(),
                                selectDepartment.getValue(),
                                selectSpecialty.getValue(),
                                Integer.parseInt(selectCourse.getValue())
                        )
                );
            }

        });

        selectGroup.addValueChangeListener(selectStringComponentValueChangeEvent -> {
            clearGrid();
            if (selectGroup.getValue() != null){
                selectDiscipline.setReadOnly(false);
                selectControlType.setReadOnly(true);

                selectControlType.clear();

                selectDiscipline.setItems(planService.getDisciplinesByFacultyAndDepartmentAndSpecialtyAndGroupCourseAndGroupGroupNumber
                        (
                                selectFaculty.getValue(),
                                selectDepartment.getValue(),
                                selectSpecialty.getValue(),
                                Integer.parseInt(selectCourse.getValue()),
                                Integer.parseInt(selectGroup.getValue())
                        )
                );
            }

        });

        selectDiscipline.addValueChangeListener(selectStringComponentValueChangeEvent -> {
            clearGrid();
            if (selectDiscipline.getValue() != null){
                selectControlType.setReadOnly(false);



                selectControlType.setItems(planService.getControlTypesByFacultyAndDepartmentAndSpecialtyAndGroupCourseAndGroupNumberAndDiscipline
                        (
                                selectFaculty.getValue(),
                                selectDepartment.getValue(),
                                selectSpecialty.getValue(),
                                Integer.parseInt(selectCourse.getValue()),
                                Integer.parseInt(selectGroup.getValue()),
                                selectDiscipline.getValue()
                        )
                );

                plansEntity = planService.getPlanEntityByFacultyAndDepartmentAndSpecialtyAndGroupCourseAndGroupNumberAndDiscipline
                        (
                                selectFaculty.getValue(),
                                selectDepartment.getValue(),
                                selectSpecialty.getValue(),
                                Integer.parseInt(selectCourse.getValue()),
                                Integer.parseInt(selectGroup.getValue()),
                                selectDiscipline.getValue()
                        );
            }

        });



        selectControlType.addValueChangeListener(selectStringComponentValueChangeEvent -> {
            updateGrid();

        });

        saveButton.addClickListener(buttonClickEvent -> {
            List<MarkDTO> markDTOList = new ArrayList<>();
            studentGrid.getDataProvider().fetch(new Query<>()).forEach(markDTOList::add);

            String typeControl = selectControlType.getValue();
            int part = plansEntity.getParts();

            List<MarksEntity> marksEntities = new ArrayList<>();



            if (typeControl.equals("Перший модульний контроль")){
                for (MarkDTO markDTO : markDTOList){

                    MarksEntity marksEntity = new MarksEntity();


                    marksEntity.setStudent(studentService.getStudentByStudentPIB_AndGroup(markDTO.getStudentPIB(), plansEntity.getGroup())); //todo можна використовувати план для отримання необхідних студентів, і лише для вибіркових робити пошук по плану рипмсувати id студента та за допомогою id брати у плані студента
                    marksEntity.setPlan(plansEntity);
                    marksEntity.setControlMethod(plansEntity.getFirstControl());
                    marksEntity.setSemester(plansEntity.getSemester());
                    marksEntity.setFinalGrade(Integer.parseInt(markDTO.getEnterMark()));
                    marksEntity.setLocked(markDTO.isLocked());
                    marksEntity.setLastUpdated(new Timestamp(System.currentTimeMillis()));
                    marksEntity.setLastUpdatedBy(userRepository.findByEmail(securityService.getAuthenticatedUser().getUsername()).orElseThrow());
                    marksEntities.add(marksEntity);

                    marksService.saveMark(marksEntity);
                }
            }
            if (typeControl.equals("Другий модульний контроль")){
                for (MarkDTO markDTO : markDTOList){

                    MarksEntity marksEntity = new MarksEntity();

                    marksEntity.setStudent(studentService.getStudentByStudentPIB_AndGroup(markDTO.getStudentPIB(), plansEntity.getGroup()));
                    marksEntity.setPlan(plansEntity);
                    marksEntity.setControlMethod(plansEntity.getSecondControl());
                    marksEntity.setSemester(plansEntity.getSemester());
                    marksEntity.setFinalGrade(Integer.parseInt(markDTO.getEnterMark()));
                    marksEntity.setLocked(markDTO.isLocked());
                    marksEntity.setLastUpdated(new Timestamp(System.currentTimeMillis()));
                    marksEntity.setLastUpdatedBy(userRepository.findByEmail(securityService.getAuthenticatedUser().getUsername()).orElseThrow());
                    marksEntities.add(marksEntity);

                    marksService.saveMark(marksEntity);
                }
            }
            if (
                    typeControl.equals("Залік") ||
                            typeControl.equals("Екзамен") ||
                            typeControl.equals("Курсова робота") ||
                            typeControl.equals("Курсовий проєкт")
            ) { //todo додати диференційований залік
//
                for (MarkDTO markDTO : markDTOList){

                    MarksEntity marksEntity = new MarksEntity();


                    marksEntity.setStudent(studentService.getStudentByStudentPIB_AndGroup(markDTO.getStudentPIB(), plansEntity.getGroup())); //todo можна використовувати план для отримання необхідних студентів, і лише для вибіркових робити пошук по плану рипмсувати id студента та за допомогою id брати у плані студента
                    marksEntity.setPlan(plansEntity);
                    marksEntity.setControlMethod(controlMethodService.getControlMethodByName(typeControl));
                    marksEntity.setSemester(plansEntity.getSemester());
                    marksEntity.setFinalGrade(Integer.parseInt(markDTO.getEnterMark()));
                    marksEntity.setLocked(markDTO.isLocked());
                    marksEntity.setLastUpdated(new Timestamp(System.currentTimeMillis()));
                    marksEntity.setLastUpdatedBy(userRepository.findByEmail(securityService.getAuthenticatedUser().getUsername()).orElseThrow());
                    marksEntities.add(marksEntity);

                    marksService.saveMark(marksEntity);
                }

            }

            if (typeControl.equals("Розрахункова робота") || typeControl.equals("Розрахунково-графічна робота")){
                if (part>=2){
//
                }
                if (part>=4) {
//
                }
                if (part>=6) {
//
                }
                if (part==8) {
//
                }


            }

            for (MarkDTO markDTO : markDTOList){
                System.out.println(markDTO.toString());
            }


        });

        approveButton.addClickListener(buttonClickEvent -> {
            List<MarkDTO> markDTOList = new ArrayList<>();
            studentGrid.getDataProvider().fetch(new Query<>()).forEach(markDTOList::add);

            for (MarkDTO markDTO : markDTOList){
                MarksEntity marksEntity = marksService.getMarkById(markDTO.getId());

                marksEntity.setLastUpdated(new Timestamp(System.currentTimeMillis()));
                marksEntity.setLastUpdatedBy(userRepository.findByEmail(securityService.getAuthenticatedUser().getUsername()).orElseThrow());

                setLocked(marksEntity);
                updateGrid();
            }
        });

        unlockButton.addClickListener(buttonClickEvent -> {
            List<MarkDTO> markDTOList = new ArrayList<>();
            studentGrid.getDataProvider().fetch(new Query<>()).forEach(markDTOList::add);

            for (MarkDTO markDTO : markDTOList){
                MarksEntity marksEntity = marksService.getMarkById(markDTO.getId());
                marksEntity.setLastUpdated(new Timestamp(System.currentTimeMillis()));
                marksEntity.setLastUpdatedBy(userRepository.findByEmail(securityService.getAuthenticatedUser().getUsername()).orElseThrow());

                marksEntity.setLocked(false);

                marksService.saveMark(marksEntity);
                updateGrid();
            }
        });







    }


    private void configureGrid(String typeControl, int part){

        studentGrid.removeAllColumns(); // Очищаємо всі колонки

        studentGrid.addColumn(student -> String.valueOf(studentGrid.getListDataView().getItems()
                        .toList()
                        .indexOf(student) + 1))
                .setHeader("№")
                .setFlexGrow(1).setWidth("25px");
        studentGrid.addColumn(MarkDTO::getStudentPIB).setHeader("ПІБ студента").setFlexGrow(3).setWidth("250px");




        if (typeControl.equals("Перший модульний контроль")){
//            studentGrid.addColumn(MarkDTO::getEnterMark).setHeader("Оцінка").setAutoWidth(true);
            setEnterMarkColumn();
        }
        if (typeControl.equals("Другий модульний контроль")){
            studentGrid.addColumn(MarkDTO::getMarkByFirstModule).setHeader("Перший модуль").setAutoWidth(true);
//            studentGrid.addColumn(MarkDTO::getEnterMark).setHeader("Оцінка").setAutoWidth(true);
            setEnterMarkColumn();
            studentGrid.addColumn(MarkDTO::getTotalMarkByFirstAndSecondModule).setHeader("Сума за модулі").setAutoWidth(true);
        }
        if (
                typeControl.equals("Залік") ||
                typeControl.equals("Екзамен") ||
                typeControl.equals("Курсова робота") ||
                typeControl.equals("Курсовий проєкт")
        ) { //todo додати диференційований залік
//            studentGrid.addColumn(MarkDTO::getEnterMark).setHeader("Оцінка").setAutoWidth(true);
            setEnterMarkColumn();
            studentGrid.addColumn(MarkDTO::getTotalMarkByFirstAndSecondModule).setHeader("Сума за модулі").setAutoWidth(true);
            studentGrid.addColumn(MarkDTO::getNationalGrade).setHeader("Оцінка за національною шкалою").setAutoWidth(true);
            studentGrid.addColumn(MarkDTO::getECTSGrade).setHeader("Оцінка ECTS").setAutoWidth(true);
        }

        if (typeControl.equals("Розрахункова робота") || typeControl.equals("Розрахунково-графічна робота")){
            if (part>=2){
//                studentGrid.addColumn(MarkDTO::getPartMark1).setHeader("Частина 1").setAutoWidth(true);
                setPart1();
//                studentGrid.addColumn(MarkDTO::getPartMark2).setHeader("Частина 2").setAutoWidth(true);
                setPart2();
            }
            if (part>=4) {
//                studentGrid.addColumn(MarkDTO::getPartMark3).setHeader("Частина 3").setAutoWidth(true);
                setPart3();
//                studentGrid.addColumn(MarkDTO::getPartMark4).setHeader("Частина 4").setAutoWidth(true);
                setPart4();
            }
            if (part>=6) {
//                studentGrid.addColumn(MarkDTO::getPartMark5).setHeader("Частина 5").setAutoWidth(true);
                setPart5();
//                studentGrid.addColumn(MarkDTO::getPartMark6).setHeader("Частина 6").setAutoWidth(true);
                setPart6();
            }
            if (part==8) {
//                studentGrid.addColumn(MarkDTO::getPartMark7).setHeader("Частина 7").setAutoWidth(true);
                setPart7();
//                studentGrid.addColumn(MarkDTO::getPartMark8).setHeader("Частина 8").setAutoWidth(true);
                setPart8();
            }

            studentGrid.addColumn(MarkDTO::getTotalGrade).setHeader("Оцінка").setAutoWidth(true);
        }



        studentGrid.addComponentColumn(markDTO -> {
            Span icon = new Span(markDTO.isLocked() ? "+" : "−"); // "+" якщо true, "−" якщо false

            // Встановлюємо стиль: колір + непрозорість (щоб був не яскравий)
            icon.getStyle()
                    .set("color", markDTO.isLocked() ? "green" : "red")  // Колір: зелений або червоний
                    .set("font-size", "20px") // Розмір символу
                    .set("font-weight", "bold") // Жирний текст
                    .set("opacity", "0.7"); // Напівпрозорість (щоб не був яскравим)

            return icon;
        }).setHeader("Чи заблоковано").setAutoWidth(true);

        studentGrid.addColumn(MarkDTO::getLastUpdated).setHeader("Час зміни").setAutoWidth(true);
        studentGrid.addColumn(MarkDTO::getLastUpdatedBy).setHeader("Користувач").setAutoWidth(true);

        studentGrid.setSizeFull();


    }

    private void setEnterMarkColumn() {
        studentGrid.addComponentColumn(markDTO -> {
            NumberField numberField = new NumberField();

            // Перевіряємо, чи значення не є null перед викликом isEmpty()
            if (markDTO.getEnterMark() != null && !markDTO.getEnterMark().isEmpty()) {
                numberField.setValue(Double.valueOf(markDTO.getEnterMark()));
            } else {
                numberField.setValue(null);
            }

            numberField.addValueChangeListener(event -> {
                if (event.getValue() != null) {
                    markDTO.setEnterMark(String.valueOf(event.getValue().intValue())); // Оновлюємо значення у MarkDTO
                } else {
                    markDTO.setEnterMark(""); // Уникаємо null, замість нього буде порожній рядок
                }
            });

            return numberField;
        }).setHeader("Оцінка").setFlexGrow(1).setWidth("50px");
    }

    private void setPart1(){
        studentGrid.addComponentColumn(markDTO -> {
            NumberField numberField = new NumberField();

            // Перевіряємо, чи значення не є null перед викликом isEmpty()
            if (markDTO.getPartMark1() != null && !markDTO.getPartMark1().isEmpty()) {
                numberField.setValue(Double.valueOf(markDTO.getPartMark1()));
            } else {
                numberField.setValue(null);
            }

            numberField.addValueChangeListener(event -> {
                if (event.getValue() != null) {
                    markDTO.setPartMark1(String.valueOf(event.getValue().intValue())); // Оновлюємо значення у MarkDTO
                } else {
                    markDTO.setPartMark1(""); // Уникаємо null, замість нього буде порожній рядок
                }
            });

            return numberField;
        }).setHeader("Частина 1").setAutoWidth(true);
    }

    private void setPart2(){
        studentGrid.addComponentColumn(markDTO -> {
            NumberField numberField = new NumberField();

            // Перевіряємо, чи значення не є null перед викликом isEmpty()
            if (markDTO.getPartMark2() != null && !markDTO.getPartMark2().isEmpty()) {
                numberField.setValue(Double.valueOf(markDTO.getPartMark2()));
            } else {
                numberField.setValue(null);
            }

            numberField.addValueChangeListener(event -> {
                if (event.getValue() != null) {
                    markDTO.setPartMark2(String.valueOf(event.getValue().intValue())); // Оновлюємо значення у MarkDTO
                } else {
                    markDTO.setPartMark2(""); // Уникаємо null, замість нього буде порожній рядок
                }
            });

            return numberField;
        }).setHeader("Частина 2").setAutoWidth(true);
    }

    private void setPart3(){
        studentGrid.addComponentColumn(markDTO -> {
            NumberField numberField = new NumberField();

            // Перевіряємо, чи значення не є null перед викликом isEmpty()
            if (markDTO.getPartMark3() != null && !markDTO.getPartMark3().isEmpty()) {
                numberField.setValue(Double.valueOf(markDTO.getPartMark3()));
            } else {
                numberField.setValue(null);
            }

            numberField.addValueChangeListener(event -> {
                if (event.getValue() != null) {
                    markDTO.setPartMark3(String.valueOf(event.getValue().intValue())); // Оновлюємо значення у MarkDTO
                } else {
                    markDTO.setPartMark3(""); // Уникаємо null, замість нього буде порожній рядок
                }
            });

            return numberField;
        }).setHeader("Частина 3").setAutoWidth(true);
    }

    private void setPart4(){
        studentGrid.addComponentColumn(markDTO -> {
            NumberField numberField = new NumberField();

            // Перевіряємо, чи значення не є null перед викликом isEmpty()
            if (markDTO.getPartMark4() != null && !markDTO.getPartMark4().isEmpty()) {
                numberField.setValue(Double.valueOf(markDTO.getPartMark4()));
            } else {
                numberField.setValue(null);
            }

            numberField.addValueChangeListener(event -> {
                if (event.getValue() != null) {
                    markDTO.setPartMark4(String.valueOf(event.getValue().intValue())); // Оновлюємо значення у MarkDTO
                } else {
                    markDTO.setPartMark4(""); // Уникаємо null, замість нього буде порожній рядок
                }
            });

            return numberField;
        }).setHeader("Частина 4").setAutoWidth(true);
    }

    private void setPart5(){
        studentGrid.addComponentColumn(markDTO -> {
            NumberField numberField = new NumberField();

            // Перевіряємо, чи значення не є null перед викликом isEmpty()
            if (markDTO.getPartMark5() != null && !markDTO.getPartMark5().isEmpty()) {
                numberField.setValue(Double.valueOf(markDTO.getPartMark5()));
            } else {
                numberField.setValue(null);
            }

            numberField.addValueChangeListener(event -> {
                if (event.getValue() != null) {
                    markDTO.setPartMark5(String.valueOf(event.getValue().intValue())); // Оновлюємо значення у MarkDTO
                } else {
                    markDTO.setPartMark5(""); // Уникаємо null, замість нього буде порожній рядок
                }
            });

            return numberField;
        }).setHeader("Частина 5").setAutoWidth(true);
    }

    private void setPart6(){
        studentGrid.addComponentColumn(markDTO -> {
            NumberField numberField = new NumberField();

            // Перевіряємо, чи значення не є null перед викликом isEmpty()
            if (markDTO.getPartMark6() != null && !markDTO.getPartMark6().isEmpty()) {
                numberField.setValue(Double.valueOf(markDTO.getPartMark6()));
            } else {
                numberField.setValue(null);
            }

            numberField.addValueChangeListener(event -> {
                if (event.getValue() != null) {
                    markDTO.setPartMark6(String.valueOf(event.getValue().intValue())); // Оновлюємо значення у MarkDTO
                } else {
                    markDTO.setPartMark6(""); // Уникаємо null, замість нього буде порожній рядок
                }
            });

            return numberField;
        }).setHeader("Частина 6").setAutoWidth(true);
    }

    private void setPart7(){
        studentGrid.addComponentColumn(markDTO -> {
            NumberField numberField = new NumberField();

            // Перевіряємо, чи значення не є null перед викликом isEmpty()
            if (markDTO.getPartMark7() != null && !markDTO.getPartMark7().isEmpty()) {
                numberField.setValue(Double.valueOf(markDTO.getPartMark7()));
            } else {
                numberField.setValue(null);
            }

            numberField.addValueChangeListener(event -> {
                if (event.getValue() != null) {
                    markDTO.setPartMark7(String.valueOf(event.getValue().intValue())); // Оновлюємо значення у MarkDTO
                } else {
                    markDTO.setPartMark7(""); // Уникаємо null, замість нього буде порожній рядок
                }
            });

            return numberField;
        }).setHeader("Частина 7").setAutoWidth(true);
    }

    private void setPart8(){
        studentGrid.addComponentColumn(markDTO -> {
            NumberField numberField = new NumberField();

            // Перевіряємо, чи значення не є null перед викликом isEmpty()
            if (markDTO.getPartMark8() != null && !markDTO.getPartMark8().isEmpty()) {
                numberField.setValue(Double.valueOf(markDTO.getPartMark8()));
            } else {
                numberField.setValue(null);
            }

            numberField.addValueChangeListener(event -> {
                if (event.getValue() != null) {
                    markDTO.setPartMark8(String.valueOf(event.getValue().intValue())); // Оновлюємо значення у MarkDTO
                } else {
                    markDTO.setPartMark8(""); // Уникаємо null, замість нього буде порожній рядок
                }
            });

            return numberField;
        }).setHeader("Частина 8").setAutoWidth(true);
    }

    private void clearGrid(){
        studentGrid.removeAllColumns();
    }

    private void setLocked(MarksEntity marksEntity){
        marksEntity.setLocked(true);
        marksService.saveMark(marksEntity);
    }

    private void updateGrid(){
        if (selectControlType.getValue() != null){


            List<MarksEntity> marksEntity = marksService.findMarksByPlan(plansEntity);
            List<MarkDTO> markDTOList1 = new ArrayList<>();

            configureGrid(selectControlType.getValue(), plansEntity.getParts());

            if (marksEntity != null && !marksEntity.isEmpty()){
                for (MarksEntity mark : marksEntity){
                    MarkDTO markDTO = new MarkDTO();
                    markDTO.setId(mark.getId());
                    markDTO.setStudentPIB(mark.getStudent().getFullName());
                    markDTO.setEnterMark(String.valueOf(mark.getFinalGrade()));
                    markDTO.setLocked(mark.isLocked());
                    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                    markDTO.setLastUpdated(formatter.format(mark.getLastUpdated()));
                    markDTO.setLastUpdatedBy(mark.getLastUpdatedBy().getLastname() + " " + mark.getLastUpdatedBy().getFirstname() + " " + mark.getLastUpdatedBy().getPatronymic());
                    markDTOList1.add(markDTO);
                }

                studentGrid.setItems(markDTOList1);
            } else {

                StudentGroupEntity studentGroupEntity = plansEntity.getGroup();

                List<StudentEntity> studentEntities;

                System.out.println(plansEntity.isElective());
                if (plansEntity.isElective()){
                    studentEntities =  studentPlansService.getStudentByPlan(plansEntity);
                } else {
                    studentEntities = studentService.getStudentByGroupId(studentGroupEntity.getId());
                }

                List<MarkDTO> markDTOList = new ArrayList<>();

                long id = 1;

                for (StudentEntity student : studentEntities){
                    MarkDTO markDTO = new MarkDTO();
                    markDTO.setId(id);
                    markDTO.setStudentPIB(student.getSurname() + " " + student.getName() + " " + student.getPatronymic());
                    markDTO.setLocked(false);
                    markDTO.setLastUpdated("");
                    markDTO.setLastUpdatedBy("");
                    markDTOList.add(markDTO);
                    id++;
                }


                studentGrid.setItems(markDTOList);

            }




        }
    }


}