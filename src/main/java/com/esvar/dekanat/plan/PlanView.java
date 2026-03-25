package com.esvar.dekanat.plan;

/*
    Цей блок містить імпорт необхідних класів та анотацій для класу PlanView.
    Він визначає основні метадані, такі як права доступу (@PermitAll),
    заголовок сторінки (@PageTitle) та маршрут (@Route).
*/

import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.dto.StudentOptionDTO;
import com.esvar.dekanat.entity.*;
import com.esvar.dekanat.mark.EnterMarksView;
import com.esvar.dekanat.plan.dialog.PlanDialog;
import com.esvar.dekanat.service.*;
import com.esvar.dekanat.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.text.Collator;
import java.util.*;
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
    private final GroupService groupService;
    private final PlanService planService;
    private final MarksPartsService marksPartsService;
    private final StudentPlansService studentPlansService;
    private final MarksService marksService;
    private final ControlPartsService controlPartsService;
    private final StudentService studentService;
    private final MarksInitializerService marksInitializerService;
    private final SummaryReportService summaryReportService;
    private static final Logger log = LoggerFactory.getLogger(EnterMarksView.class);

    // UI-компоненти
    private final ComboBox<GroupDTO> selectGroup = new ComboBox<>(); // Вибір групи
    private final Select<String> sessionSelect = new Select<>(); // Вибір сесії
    private final Button firstModuleButton = new Button("І модульний контроль");
    private final Button secondModuleButton = new Button("ІІ модульний контроль");
    private final Button semesterControlButton = new Button("Семестровий");
    private final Button addButton = new Button("Додати"); // Кнопка додавання плану
    private final Grid<PlansEntity> planGrid = new Grid<>(PlansEntity.class, false); // Таблиця планів

    // Діалогове вікно для створення/редагування планів
    private final PlanDialog planDialog;
    public PlanView(DisciplineService disciplineService, DepartmentService departmentService,
                    ControlMethodService controlMethodService,
                    GroupService groupService, PlanService planService,
                    MarksPartsService marksPartsService, StudentPlansService studentPlansService,
                    MarksService marksService, ControlPartsService controlPartsService, StudentService studentService, MarksInitializerService marksInitializerService, SummaryReportService summaryReportService) {
        // Ініціалізація сервісів
        this.disciplineService = disciplineService;
        this.departmentService = departmentService;
        this.controlMethodService = controlMethodService;
        this.groupService = groupService;
        this.planService = planService;
        this.marksPartsService = marksPartsService;
        this.studentPlansService = studentPlansService;
        this.marksService = marksService;
        this.controlPartsService = controlPartsService;
        this.studentService = studentService;
        this.marksInitializerService = marksInitializerService;
        this.summaryReportService = summaryReportService;

        // Ініціалізація діалогового вікна
        List<String> disciplines = disciplineService.getAllDisciplines().stream()
                .map(DisciplineEntity::getTitle).collect(Collectors.toList());
        List<String> departments = departmentService.getAllDepartments().stream()
                .map(DepartmentEntity::getTitle).collect(Collectors.toList());
        List<String> firstControlTypes = controlMethodService.getTypeControlMethod(1).stream()
                .map(ControlMethodEntity::getName).collect(Collectors.toList());
        List<String> secondControlTypes = controlMethodService.getTypeControlMethod(2).stream()
                .map(ControlMethodEntity::getName)
                .collect(Collectors.toCollection(ArrayList::new));
        if (!secondControlTypes.contains("Відсутній")) {
            secondControlTypes.add(0, "Відсутній");
        }
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
        selectGroup.setLabel("Група");
        Collator collator = Collator.getInstance(new Locale("uk", "UA"));
        selectGroup.setItems(
                groupService.getGroupsDTO()
        );

        selectGroup.addValueChangeListener(event -> {
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

        configureReportButtons();
    }

    private void setupLayout() {
        HorizontalLayout filterLayout = new HorizontalLayout(selectGroup, sessionSelect, createReportGenerationBlock());
        filterLayout.getStyle().set("padding", "20px 20px 0px 20px");
        filterLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        filterLayout.setWidthFull();

        HorizontalLayout gridLayout = new HorizontalLayout(planGrid);
        gridLayout.getStyle().set("padding", "20px");
        gridLayout.setHeight("80%");

        setHeight("90%");
        add(filterLayout, gridLayout, addButton);
    }

    private void openCreateDialog() {
        GroupDTO selectedGroup = selectGroup.getValue();
        if (selectedGroup != null) {
            List<StudentOptionDTO> students = groupService.getStudentOptionsForGroup(selectedGroup.getGroupCode());
            planDialog.updateStudentsList(students);
        } else {
            planDialog.updateStudentsList(Collections.emptyList());
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


        List<Long> selectedStudents = isElective
                ? studentPlansService.getStudentIdsByPlan(plan) // Отримуємо студентів з student_plans
                : new ArrayList<>();

        // Відкриваємо діалог для оновлення з передачею ID плану
        planDialog.openForUpdate(disciplineName, hours, isElective,
                firstControlType, secondControlType, parts, departmentName, selectedStudents, plan.getId());
    }

    private void saveNewPlan(String discipline, int hours, boolean isElective,
                             String firstControl, String secondControl, String parts,
                             String department, List<Long> students) {
        PlansEntity newPlan = new PlansEntity();
        newPlan.setDiscipline(disciplineService.getDisciplineByTitle(discipline));
        newPlan.setHours(hours);
        newPlan.setElective(isElective);
        newPlan.setFirstControl(controlMethodService.getControlMethodByName(firstControl));
        newPlan.setSecondControl(controlMethodService.getControlMethodByName(secondControl));
        newPlan.setDepartment(departmentService.getDepartmentByTitle(department));
        StudentGroupEntity selectedGroup = getSelectedGroup();
        if (selectedGroup == null || selectedGroup.getSpecialty() == null
                || selectedGroup.getSpecialty().getFaculty() == null) {
            return;
        }
        newPlan.setGroup(selectedGroup); // legacy support
        newPlan.addGroup(selectedGroup);
        newPlan.setSpecialty(selectedGroup.getSpecialty());
        newPlan.setSemester(getSelectedSemester());
        newPlan.setParts(Integer.parseInt(parts));
        newPlan.setFaculty(selectedGroup.getSpecialty().getFaculty());
        planService.savePlan(newPlan);
        List<StudentGroupEntity> allowedGroups = selectedGroup == null ? Collections.emptyList() : List.of(selectedGroup);
        List<Long> targetStudentIds;
        if (isElective && students != null && !students.isEmpty()) {
            targetStudentIds = new ArrayList<>(students);
        } else {
            targetStudentIds = selectedGroup == null
                    ? Collections.emptyList()
                    : studentService.getStudentByGroupId(selectedGroup.getId()).stream()
                    .map(StudentEntity::getId)
                    .toList();
        }

        List<StudentEntity> targetStudents;
        try {
            targetStudents = studentPlansService.synchronizePlanAssignments(newPlan, targetStudentIds, allowedGroups);
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage());
            return;
        }
        marksInitializerService.initializeMarksForPlan(newPlan, targetStudents);


        updateGrid();
    }

    private void updateExistingPlan(Long planId, String discipline, int hours, boolean isElective,
                                    String firstControl, String secondControl, String parts,
                                    String department, List<Long> students) {
        // Отримуємо план за ID
        PlansEntity updatedPlan = planService.getPlanById(planId);
        if (updatedPlan == null) return;

        List<StudentEntity> beforeStudents = studentPlansService.getStudentByPlan(updatedPlan);
        ControlMethodEntity prevFirst = updatedPlan.getFirstControl();
        ControlMethodEntity prevSecond = updatedPlan.getSecondControl();
        updatedPlan.setElective(isElective);

        // Оновлюємо дані
        updatedPlan.setDiscipline(disciplineService.getDisciplineByTitle(discipline));
        updatedPlan.setHours(hours);
        updatedPlan.setFirstControl(controlMethodService.getControlMethodByName(firstControl));
        updatedPlan.setSecondControl(controlMethodService.getControlMethodByName(secondControl));
        updatedPlan.setDepartment(departmentService.getDepartmentByTitle(department));
        updatedPlan.setParts(Integer.parseInt(parts));

        // Зберігаємо оновлення
        planService.updatePlan(updatedPlan);

        List<StudentGroupEntity> allowedGroups = new ArrayList<>(getAllowedGroupsForPlan(updatedPlan));
        List<StudentEntity> targetStudents;
        try {
            if (isElective) {
                List<Long> studentIds = students == null ? Collections.emptyList() : new ArrayList<>(students);
                targetStudents = studentPlansService.synchronizePlanAssignments(updatedPlan, studentIds, allowedGroups);
            } else {
                List<StudentEntity> groupStudents = getStudentsForPlanGroups(updatedPlan);
                if (groupStudents.isEmpty()) {
                    StudentGroupEntity selectedGroup = getSelectedGroup();
                    groupStudents = selectedGroup == null
                            ? Collections.emptyList()
                            : studentService.getStudentByGroupId(selectedGroup.getId());
                    if (allowedGroups.isEmpty() && selectedGroup != null) {
                        allowedGroups = new ArrayList<>(List.of(selectedGroup));
                    }
                }
                List<Long> groupStudentIds = groupStudents.stream()
                        .map(StudentEntity::getId)
                        .toList();
                targetStudents = studentPlansService.synchronizePlanAssignments(updatedPlan, groupStudentIds, allowedGroups);
            }
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage());
            return;
        }

        List<Long> beforeIds = beforeStudents.stream().map(StudentEntity::getId).toList();
        List<Long> afterIds = targetStudents.stream().map(StudentEntity::getId).toList();

        List<Long> removedIds = beforeIds.stream()
                .filter(id -> !afterIds.contains(id))
                .toList();
        List<StudentEntity> addedStudents = targetStudents.stream()
                .filter(s -> !beforeIds.contains(s.getId()))
                .toList();

        if (!removedIds.isEmpty()) {
            marksPartsService.deleteByPlanIdAndStudentIds(updatedPlan.getId(), removedIds);
            marksService.deleteByPlanIdAndStudentIds(updatedPlan.getId(), removedIds);
        }

        if (!addedStudents.isEmpty()) {
            marksInitializerService.initializeMarksForPlan(updatedPlan, addedStudents);
        }

        if (!prevFirst.getId().equals(updatedPlan.getFirstControl().getId())) {
            marksPartsService.transferControlMethod(updatedPlan, prevFirst, updatedPlan.getFirstControl());
        }
        if (prevSecond != null && updatedPlan.getSecondControl() != null
                && !prevSecond.getId().equals(updatedPlan.getSecondControl().getId())) {
            marksPartsService.transferControlMethod(updatedPlan, prevSecond, updatedPlan.getSecondControl());
        }

        // Обробка частин (РР/РГР) - видаляємо зайві частини й перераховуємо фінальні оцінки
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
        GroupDTO selectedGroup = selectGroup.getValue();
        if (selectedGroup == null) {
            planDialog.updateStudentsList(new ArrayList<>());
            return;
        }

        List<StudentOptionDTO> students = groupService.getStudentOptionsForGroup(selectedGroup.getGroupCode());
        planDialog.updateStudentsList(students);
    }

    private void updateGrid() {
        GroupDTO selectedGroup = selectGroup.getValue();
        String selectedSession = sessionSelect.getValue();
        if (selectedGroup == null || selectedSession == null) {
            planGrid.setItems(planService.getAllPlansForGroupAndSemester(null, 0));
            return;
        }

        planGrid.setItems(planService.getAllPlansForGroupAndSemester(groupService.getGroupByTitle(selectedGroup.getGroupCode()), getSelectedSemester()));
    }

    private int getSelectedSemester() {
        GroupDTO selectedGroup = selectGroup.getValue();
        String selectedSession = sessionSelect.getValue();
        if (selectedGroup == null || selectedSession == null) {
            return 1; // За замовчуванням - перший семестр
        }
        return getNumberSemester(selectedGroup.getGroupCode(), selectedSession);
    }

    private int getNumberSemester(String groupTitle, String semester) {
        String[] groupParts = groupTitle.split("-");
        if (semester.equals("Зимова")) {
            return (Integer.parseInt(groupParts[1]) * 2 - 1);
        } else {
            return Integer.parseInt(groupParts[1]) * 2;
        }
    }

    private StudentGroupEntity getSelectedGroup() {
        GroupDTO selectedGroup = selectGroup.getValue();
        if (selectedGroup == null) {
            return null;
        }
        return groupService.getGroupByTitle(selectedGroup.getGroupCode());
    }

    private List<StudentEntity> getStudentsForPlanGroups(PlansEntity plan) {
        List<StudentGroupEntity> groups = getPlanGroups(plan);
        if (groups.isEmpty()) {
            return Collections.emptyList();
        }

        return groups.stream()
                .flatMap(group -> studentService.getStudentByGroupId(group.getId()).stream())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<StudentGroupEntity> getPlanGroups(PlansEntity plan) {
        if (plan == null || plan.getId() == null) {
            return Collections.emptyList();
        }

        PlansEntity planWithGroups = planService.getPlanWithGroups(plan.getId());
        if (planWithGroups == null || planWithGroups.getGroups() == null || planWithGroups.getGroups().isEmpty()) {
            return Collections.emptyList();
        }

        return planWithGroups.getGroups().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<StudentGroupEntity> getAllowedGroupsForPlan(PlansEntity plan) {
        List<StudentGroupEntity> groups = getPlanGroups(plan);
        if (!groups.isEmpty()) {
            return groups;
        }

        StudentGroupEntity selectedGroup = getSelectedGroup();
        return selectedGroup == null ? Collections.emptyList() : List.of(selectedGroup);
    }

    private void configureReportButtons() {
        List<Button> reportButtons = List.of(firstModuleButton, secondModuleButton, semesterControlButton);
        reportButtons.forEach(button -> button.addThemeVariants(ButtonVariant.LUMO_CONTRAST));

        firstModuleButton.setTooltipText("Зведений звіт за перший модульний контроль");
        secondModuleButton.setTooltipText("Зведений звіт за другий модульний контроль");
        semesterControlButton.setTooltipText("Зведений звіт за семестровий контроль");

        firstModuleButton.addClickListener(buttonClickEvent -> {
            generateFirstModuleSummaryReport();
            Notification.show("Генерація першого модулю");
        });
        secondModuleButton.addClickListener(buttonClickEvent -> {
            generateSecondModuleSummaryReport();
        });
        semesterControlButton.addClickListener(buttonClickEvent -> generateSemesterSummaryReport());

        firstModuleButton.setIcon(VaadinIcon.FILE_TEXT.create());
        secondModuleButton.setIcon(VaadinIcon.FILE_PROCESS.create());
        semesterControlButton.setIcon(VaadinIcon.FILE_PRESENTATION.create());
        semesterControlButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
    }

    private void generateFirstModuleSummaryReport() {
        try {
            SummaryReportService.SummaryReportResult result = summaryReportService.generateFirstModuleReport(selectGroup.getValue(), isWinterSessionSelected());
            String fileName = String.format("summary-first-module-%s.pdf", result.groupCode());
            openPdfReport(fileName, result.pdfBytes());

            Notification notification = Notification.show("Звіт сформовано");
            notification.setDuration(3000);
        } catch (SummaryReportService.SummaryReportGenerationException ex) {
            Notification.show(ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Не вдалося згенерувати зведений звіт", ex);
            Notification.show("Не вдалося згенерувати звіт");
        }
    }

    private void generateSecondModuleSummaryReport() {
        try {
            SummaryReportService.SummaryReportResult result = summaryReportService.generateSecondModuleReport(selectGroup.getValue(), isWinterSessionSelected());
            String fileName = String.format("summary-second-module-%s.pdf", result.groupCode());
            openPdfReport(fileName, result.pdfBytes());

            Notification notification = Notification.show("Звіт сформовано");
            notification.setDuration(3000);
        } catch (SummaryReportService.SummaryReportGenerationException ex) {
            Notification.show(ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Не вдалося згенерувати зведений звіт", ex);
            Notification.show("Не вдалося згенерувати звіт");
        }
    }

    private void generateSemesterSummaryReport() {
        try {
            SummaryReportService.SummaryReportResult result = summaryReportService.generateSemesterReport(selectGroup.getValue(), isWinterSessionSelected());
            String fileName = String.format("summary-semester-%s.pdf", result.groupCode());
            openPdfReport(fileName, result.pdfBytes());

            Notification notification = Notification.show("Звіт сформовано");
            notification.setDuration(3000);
        } catch (SummaryReportService.SummaryReportGenerationException ex) {
            Notification.show(ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Не вдалося згенерувати зведений звіт", ex);
            Notification.show("Не вдалося згенерувати звіт");
        }
    }

    private void openPdfReport(String fileName, byte[] pdfBytes) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            throw new IllegalStateException("UI is not available for opening the PDF report");
        }

        StreamResource resource = new StreamResource(fileName, () -> new ByteArrayInputStream(pdfBytes));
        resource.setContentType("application/pdf");
        resource.setCacheTime(0);

        StreamRegistration registration = ui.getSession()
                .getResourceRegistry()
                .registerResource(resource);

        String resourceUrl = registration.getResourceUri().toString();
        ui.getPage().open(resourceUrl, "_blank");
    }

    private boolean isWinterSessionSelected() {
        return "Зимова".equals(sessionSelect.getValue());
    }

    private Div createReportGenerationBlock() {
        Span title = new Span("Генерація відомостей");
        title.getStyle()
                .set("fontWeight", "600")
                .set("fontSize", "var(--lumo-font-size-s)");

        HorizontalLayout buttonsRow = new HorizontalLayout(firstModuleButton, secondModuleButton, semesterControlButton);
        buttonsRow.setSpacing(true);
        buttonsRow.setPadding(false);
        buttonsRow.setWidthFull();

        VerticalLayout content = new VerticalLayout(title, buttonsRow);
        content.setPadding(false);
        content.setSpacing(true);
        content.setMargin(false);

        Div container = new Div(content);
        container.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("borderRadius", "var(--lumo-border-radius-m)")
                .set("padding", "12px 16px")
                .set("background", "var(--lumo-base-color)")
                .set("boxShadow", "var(--lumo-box-shadow-s)")
                .set("marginLeft", "auto");

        return container;
    }

}
