package com.esvar.dekanat.plan;

/*
    Цей блок містить імпорт необхідних класів та анотацій для класу PlanView.
    Він визначає основні метадані, такі як права доступу (@PermitAll),
    заголовок сторінки (@PageTitle) та маршрут (@Route).
*/

import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.entity.*;
import com.esvar.dekanat.plan.dialog.PlanDialog;
import com.esvar.dekanat.service.*;
import com.esvar.dekanat.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

//todo після оновлення студентів що обрали дисципліну, при повторному вході в діалог вибору студентів, відображаються всі студенти, хоча в бд записи правильні(проблема на стороні фронта)

@PermitAll // Дозволяє доступ до сторінки всім користувачам
@PageTitle("Навчальні плани | Деканат") // Заголовок сторінки
@Route(value = "", layout = MainLayout.class) // Маршрут та макет сторінки

/*
    Цей блок містить оголошення класу PlanView та його полів.
    Поля включають сервіси для роботи з базою даних,
    UI-компоненти (наприклад, Grid, Select, Button)
    та діалогове вікно PlanDialog.
*/
public class PlanView extends Div {
    // Сервіси для взаємодії з базою даних
    private final DisciplineService disciplineService;
    private final DepartmentService departmentService;
    private final ControlMethodService controlMethodService;
    private final SpecialtyService specialtyService;
    private final GroupService groupService;
    private final PlanService planService;
    private final MarksPartsService marksPartsService;
    private final StudentPlansService studentPlansService;
    private final MarksService marksService;
    private final ControlPartsService controlPartsService;
    private final StudentService studentService;

    // UI-компоненти
    private final Select<String> groupSelect = new Select<>(); // Вибір групи
    private final Select<String> sessionSelect = new Select<>(); // Вибір сесії
    private final Button addButton = new Button("Додати"); // Кнопка додавання плану
    private final Grid<PlansEntity> planGrid = new Grid<>(PlansEntity.class, false); // Таблиця планів

    // Діалогове вікно для створення/редагування планів
    private final PlanDialog planDialog;
    public PlanView(DisciplineService disciplineService, DepartmentService departmentService,
                    ControlMethodService controlMethodService, SpecialtyService specialtyService,
                    GroupService groupService, PlanService planService,
                    MarksPartsService marksPartsService, StudentPlansService studentPlansService,
                    MarksService marksService, ControlPartsService controlPartsService, StudentService studentService) {
        // Ініціалізація сервісів
        this.disciplineService = disciplineService;
        this.departmentService = departmentService;
        this.controlMethodService = controlMethodService;
        this.specialtyService = specialtyService;
        this.groupService = groupService;
        this.planService = planService;
        this.marksPartsService = marksPartsService;
        this.studentPlansService = studentPlansService;
        this.marksService = marksService;
        this.controlPartsService = controlPartsService;
        this.studentService = studentService;

        // Ініціалізація діалогового вікна
        List<String> disciplines = disciplineService.getAllDisciplines().stream()
                .map(DisciplineEntity::getTitle).collect(Collectors.toList());
        List<String> departments = departmentService.getAllDepartments().stream()
                .map(DepartmentEntity::getTitle).collect(Collectors.toList());
        List<String> firstControlTypes = controlMethodService.getTypeControlMethod(1).stream()
                .map(ControlMethodEntity::getName).collect(Collectors.toList());
        List<String> secondControlTypes = controlMethodService.getTypeControlMethod(2).stream()
                .map(ControlMethodEntity::getName).collect(Collectors.toList());
        planDialog = new PlanDialog(disciplines, departments, firstControlTypes, secondControlTypes, new ArrayList<>());

        // Встановлення слухачів для збереження/оновлення плану
        planDialog.setSavePlanListener(this::saveNewPlan);
        planDialog.setUpdatePlanListener(this::updateExistingPlan);
        planDialog.setRemovePlanListener(this::deletePlan);

        // Ініціалізація компонентів та розмітки
        initializeComponents();
        setupLayout();
    }

    private void initializeComponents() {
        // Налаштування кнопки "Додати"
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.getStyle().set("margin", "20px");
        addButton.addClickListener(event -> openCreateDialog());

        int i = 0;

        // Налаштування таблиці планів
        planGrid.addColumn(plan -> String.valueOf(planGrid.getListDataView().getItems()
                        .toList()
                        .indexOf(plan) + 1))
                .setHeader("№")
                .setAutoWidth(true);

        planGrid.addColumn(plan -> plan.getDiscipline().getTitle()).setHeader("Дисципліна");
        planGrid.addColumn(plan -> String.valueOf(plan.getHours())).setHeader("Години");
        planGrid.addColumn(plan -> plan.isElective() ? "Так" : "Ні").setHeader("Вибіркова");
        planGrid.addColumn(plan -> plan.getFirstControl().getName()).setHeader("Перший к.");
        planGrid.addColumn(plan -> plan.getSecondControl() != null ? plan.getSecondControl().getName() : "").setHeader("Другий к.");
        planGrid.addColumn(plan -> plan.getDepartment().getAbbreviation()).setHeader("Кафедра");
        planGrid.addComponentColumn(plan -> {
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addClickListener(event -> openEditDialog(plan));
            return editButton;
        }).setHeader("Дії");
        planGrid.setHeight("100%");

        // Налаштування вибору групи
        groupSelect.setLabel("Група");
        groupSelect.setItems(groupService.getGroupsDTO().stream().map(GroupDTO::toString).collect(Collectors.toList()));
        groupSelect.addValueChangeListener(event -> {
            updateStudentListInDialog();
            updateGrid();
        });

        // Налаштування вибору сесії
        sessionSelect.setLabel("Сессія");
        sessionSelect.setItems("Зимова", "Літня");
        sessionSelect.setValue("Зимова"); // За замовчуванням - зимова сесія
        sessionSelect.addValueChangeListener(event -> {
            updateStudentListInDialog();
            updateGrid();
        });
    }

    private void setupLayout() {
        HorizontalLayout filterLayout = new HorizontalLayout(groupSelect, sessionSelect);
        filterLayout.getStyle().set("padding", "20px 20px 0px 20px");

        HorizontalLayout gridLayout = new HorizontalLayout(planGrid);
        gridLayout.getStyle().set("padding", "20px");
        gridLayout.setHeight("80%");

        setHeight("90%");
        add(filterLayout, gridLayout, addButton);
    }

    private void openCreateDialog() {
        String selectedGroup = groupSelect.getValue();
        if (selectedGroup != null) {
            List<String> students = groupService.getAllStudentsForSelectedGroup(selectedGroup);
            planDialog.updateStudentsList(students);
        }
        planDialog.openForCreation();
    }

    private void openEditDialog(PlansEntity plan) {
        // Отримуємо дані про поточний план
        String disciplineName = plan.getDiscipline().getTitle();
        int hours = plan.getHours();
        boolean isElective = plan.isElective();
        String firstControlType = plan.getFirstControl().getName();
        String secondControlType = plan.getSecondControl() != null ? plan.getSecondControl().getName() : "Відсутній";
        String departmentName = plan.getDepartment().getTitle();
        String parts = String.valueOf(plan.getParts()); // За замовчуванням


        List<String> selectedStudents = isElective
                ? planService.getSelectedStudentsForPlan(plan) // Отримуємо студентів з student_plans
                : new ArrayList<>();

        // Відкриваємо діалог для оновлення з передачею ID плану
        planDialog.openForUpdate(disciplineName, hours, isElective,
                firstControlType, secondControlType, parts, departmentName, selectedStudents, plan.getId());
    }

    private void saveNewPlan(String discipline, int hours, boolean isElective,
                             String firstControl, String secondControl, String parts,
                             String department, List<String> students) {
        PlansEntity newPlan = new PlansEntity();
        newPlan.setDiscipline(disciplineService.getDisciplineByTitle(discipline));
        newPlan.setHours(hours);
        newPlan.setElective(isElective);
        newPlan.setFirstControl(controlMethodService.getControlMethodByName(firstControl));
        newPlan.setSecondControl(controlMethodService.getControlMethodByName(secondControl));
        newPlan.setDepartment(departmentService.getDepartmentByTitle(department));
        newPlan.setSpecialty(getSelectedSpecialty());
        newPlan.setSemester(getSelectedSemester());
        newPlan.setParts(Integer.parseInt(parts));
        newPlan.setFaculty(newPlan.getSpecialty().getFaculty());
        newPlan.setGroup(groupService.getGroupByTitle(groupSelect.getValue()));
        planService.savePlan(newPlan);

        if (isElective && students != null && !students.isEmpty()) {
            for (String studentName : students) {
                StudentEntity student = studentService.getStudentByFullName(studentName.split(" ")[0],
                        studentName.split(" ")[1], studentName.split(" ")[2]);
                StudentPlansEntity studentPlan = new StudentPlansEntity();
                studentPlan.setStudent(student);
                studentPlan.setPlan(newPlan);
                studentPlansService.saveStudentPlan(studentPlan);
            }
        }


        updateGrid();
    }

    private void updateExistingPlan(Long planId, String discipline, int hours, boolean isElective,
                                    String firstControl, String secondControl, String parts,
                                    String department, List<String> students) {
        // Отримуємо план за ID
        PlansEntity updatedPlan = planService.getPlanById(planId);
        if (updatedPlan == null) return;

        if (updatedPlan.isElective() && !isElective) {
            studentPlansService.deleteByPlanId(updatedPlan.getId()); // Видаляємо записи з student_plans
        }

        // Оновлюємо дані
        updatedPlan.setDiscipline(disciplineService.getDisciplineByTitle(discipline));
        updatedPlan.setHours(hours);
        updatedPlan.setElective(isElective);
        updatedPlan.setFirstControl(controlMethodService.getControlMethodByName(firstControl));
        updatedPlan.setSecondControl(controlMethodService.getControlMethodByName(secondControl));
        updatedPlan.setDepartment(departmentService.getDepartmentByTitle(department));
        updatedPlan.setParts(Integer.parseInt(parts));

        // Зберігаємо оновлення
        planService.updatePlan(updatedPlan);

        // Якщо дисципліна вибіркова, оновлюємо записи у student_plans
        if (isElective && students != null && !students.isEmpty()) {
            studentPlansService.updateStudentPlans(updatedPlan, students);
        }

        // Обробка частин (РР/РГР) - видаляємо зайві частини і перераховуємо фінальні оцінки
        if (updatedPlan.getSecondControl().getName().equals("Розрахункова робота") ||
                updatedPlan.getSecondControl().getName().equals("Розрахунково-графічна робота")) {
            int newParts = updatedPlan.getParts(); // Нове значення кількості частин
            // Видаляємо записи MarksPartsEntity, де partNumber > newParts
            marksPartsService.deletePartsGreaterThan(updatedPlan.getId(), newParts);
            // Перераховуємо фінальні оцінки MarksEntity цього плану, використовуючи залишилися частини
            marksPartsService.updateFinalGradesForPlan(updatedPlan, newParts);
        }

        updateGrid();
    }


    private void deletePlan(Long planId) {
        planService.deletePlanById(planId);
        updateGrid();
    }

    private void updateStudentListInDialog() {
        String selectedGroup = groupSelect.getValue();
        if (selectedGroup != null) {
            List<String> students = groupService.getAllStudentsForSelectedGroup(selectedGroup);
            planDialog.updateStudentsList(students);
        } else {
            planDialog.updateStudentsList(new ArrayList<>());
        }
    }

    private void updateGrid() {
        String selectedGroup = groupSelect.getValue();
        String selectedSession = sessionSelect.getValue();
        if (selectedGroup == null || selectedSession == null) {
            planGrid.setItems(planService.getAllPlansForGroupAndSemester(null, 0));
            return;
        }

        planGrid.setItems(planService.getAllPlansForGroupAndSemester(groupService.getGroupByTitle(groupSelect.getValue()), getSelectedSemester()));
    }

    private int getSelectedSemester() {
        String selectedGroup = groupSelect.getValue();
        String selectedSession = sessionSelect.getValue();
        if (selectedGroup == null || selectedSession == null) {
            return 1; // За замовчуванням - перший семестр
        }
        return getNumberSemester(selectedGroup, selectedSession);
    }

    private int getNumberSemester(String groupTitle, String semester) {
        String[] groupParts = groupTitle.split("-");
        if (semester.equals("Зимова")) {
            return (Integer.parseInt(groupParts[1]) * 2 - 1);
        } else {
            return Integer.parseInt(groupParts[1]) * 2;
        }
    }

    private SpecialtyEntity getSelectedSpecialty() {
        String selectedGroup = groupSelect.getValue();
        if (selectedGroup == null) {
            return null;
        }
        String abbreviation = selectedGroup.split("-")[0];
        return specialtyService.getSpecialtyByAbbreviation(abbreviation);
    }

}
