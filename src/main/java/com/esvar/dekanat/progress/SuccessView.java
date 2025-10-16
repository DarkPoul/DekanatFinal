package com.esvar.dekanat.progress;

import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentPlansEntity;
import com.esvar.dekanat.service.*;
import com.esvar.dekanat.service.SyncService;
import com.esvar.dekanat.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.text.Collator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * View showing academic performance for a student.
 */
@PermitAll
@PageTitle("Успішність | Деканат")
@Route(value = "success", layout = MainLayout.class)
public class SuccessView extends Div {

    private final GroupService groupService;
    private final StudentService studentService;
    private final MarksService marksService;
    private final DisciplineService disciplineService;
    private final ControlMethodService controlMethodService;
    private final PlanService planService;
    private final StudentPlansService studentPlansService;
    private final MarksInitializerService marksInitializerService;

    private final Select<String> groupSelect = new Select<>();
    private final ComboBox<String> studentSelect = new ComboBox<>();
    private final ComboBox<String> otherStudentSelect = new ComboBox<>();
    private final Button syncButton = new Button("Синхронізувати");
    private final Button applySyncButton = new Button("Провести синхронізацію");
    private final Button cancelSyncButton = new Button("Скасувати");
    private final Grid<Row> grid = new Grid<>(Row.class, false);
    private final Grid<Row> otherGrid = new Grid<>(Row.class, false);
    private final MarkEditDialog editDialog;
    private final SyncService syncService;
    private final Collator ukrainianCollator = Collator.getInstance(new Locale("uk", "UA"));

    public SuccessView(GroupService groupService,
                       StudentService studentService,
                       MarksService marksService,
                       DisciplineService disciplineService,
                       ControlMethodService controlMethodService,
                       PlanService planService, StudentPlansService studentPlansService, MarksInitializerService marksInitializerService,
                       SyncService syncService) {
        this.groupService = groupService;
        this.studentService = studentService;
        this.marksService = marksService;
        this.disciplineService = disciplineService;
        this.controlMethodService = controlMethodService;
        this.planService = planService;
        this.studentPlansService = studentPlansService;
        this.marksInitializerService = marksInitializerService;
        this.syncService = syncService;
        this.editDialog = new MarkEditDialog(disciplineService, controlMethodService);
        configureSelectors();
        configureGrid();
        configureSyncControls();

        HorizontalLayout filters = new HorizontalLayout(groupSelect, studentSelect, syncButton,
                otherStudentSelect, applySyncButton, cancelSyncButton);
        filters.setPadding(true);
        filters.setAlignItems(FlexComponent.Alignment.BASELINE);

        HorizontalLayout tables = new HorizontalLayout(grid, otherGrid);
        tables.setWidthFull();
        tables.setFlexGrow(1, grid, otherGrid);
        tables.getStyle().set("overflow-x", "auto");

        VerticalLayout layout = new VerticalLayout(new H2("Успішність студента"), filters, tables);
        layout.setPadding(false);
        layout.setWidthFull();
        layout.getStyle().set("overflow-x", "auto");

        add(layout, editDialog);
    }


    private void configureSelectors() {
        groupSelect.setLabel("Група");
        groupSelect.setItems(groupService.getGroupsDTO().stream()
                .map(GroupDTO::toString)
                .sorted(ukrainianCollator)
                .collect(Collectors.toList()));
        groupSelect.addValueChangeListener(e -> updateStudents());

        studentSelect.setLabel("Студент");
        studentSelect.addValueChangeListener(e -> updateGrid());
        studentSelect.setClearButtonVisible(true);
    }

    private void configureGrid() {
        grid.addColumn(row -> String.valueOf(grid.getListDataView().getItems().toList().indexOf(row) + 1))
                .setHeader("№")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(Row::discipline).setHeader("Дисципліна")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(Row::semester).setHeader("Семестр")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(Row::hours).setHeader("Години")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(Row::controlType).setHeader("Тип контролю")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(Row::grade).setHeader("Оцінка")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addComponentColumn(row -> {
                    Button edit = new Button("Редагувати");
                    edit.addClickListener(e -> openEditor(row));
                    return edit;
                }).setHeader("Дії")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.setWidthFull();
        grid.getStyle().set("min-width", "max-content");
        grid.setItems(List.of());
    }

    private void configureSyncControls() {
        otherStudentSelect.setLabel("Інший студент");
        otherStudentSelect.setVisible(false);
        otherStudentSelect.addValueChangeListener(e -> updateOtherGrid());

        otherGrid.addColumn(row -> String.valueOf(otherGrid.getListDataView().getItems().toList().indexOf(row) + 1))
                .setHeader("№")
                .setAutoWidth(true)
                .setFlexGrow(0);
        otherGrid.addColumn(Row::discipline).setHeader("Дисципліна")
                .setAutoWidth(true)
                .setFlexGrow(0);
        otherGrid.addColumn(Row::semester).setHeader("Семестр")
                .setAutoWidth(true)
                .setFlexGrow(0);
        otherGrid.addColumn(Row::hours).setHeader("Години")
                .setAutoWidth(true)
                .setFlexGrow(0);
        otherGrid.addColumn(Row::controlType).setHeader("Тип контролю")
                .setAutoWidth(true)
                .setFlexGrow(0);
        otherGrid.addColumn(Row::grade).setHeader("Оцінка")
                .setAutoWidth(true)
                .setFlexGrow(0);
        otherGrid.setWidthFull();
        otherGrid.getStyle().set("min-width", "max-content");
        otherGrid.setItems(List.of());
        otherGrid.setVisible(false);

        syncButton.addClickListener(e -> enterSyncMode());
        applySyncButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        cancelSyncButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        applySyncButton.setVisible(false);
        cancelSyncButton.setVisible(false);
        applySyncButton.addClickListener(e -> performSync());
        cancelSyncButton.addClickListener(e -> exitSyncMode());
    }


    private void updateStudents() {
        String group = groupSelect.getValue();
        if (group != null) {
            List<String> students = groupService.getAllStudentsForSelectedGroup(group);
            studentSelect.setItems(students.stream().sorted(ukrainianCollator).toList());
        } else {
            studentSelect.setItems(List.of());
        }
        grid.setItems(List.of());
        exitSyncMode();
    }

    private void updateGrid() {
        String studentName = studentSelect.getValue();
        String group = groupSelect.getValue();
        if (studentName == null || group == null) {
            grid.setItems(List.of());
            return;
        }
        StudentEntity student = studentService.getStudentForCard(group, studentName);
        ensureZeroMarks(student);
        List<Row> rows = marksService.getMarksByStudent(student).stream()
                .map(m -> new Row(
                        m.getId(),
                        m.getPlan().getDiscipline().getTitle(),
                        m.getSemester(),
                        m.getPlan().getHours(),
                        m.getControlMethod().getName(),
                        m.getFinalGrade()))
                .toList();
        grid.setItems(rows);
    }

    private void openEditor(Row row) {
        editDialog.setSaveListener((disc, sem, hrs, ctrl, grade) -> saveRow(row, disc, sem, hrs, ctrl, grade));
        editDialog.open(row.discipline, row.semester, row.hours, row.controlType, row.grade);
    }

    private void saveRow(Row row, String disc, int sem, int hrs, String ctrl, int grade) {
        MarksEntity mark = marksService.getMarkById(row.id);
        if (mark == null) return;
        PlansEntity plan = mark.getPlan();
        plan.setDiscipline(disciplineService.getDisciplineByTitle(disc));
        plan.setSemester(sem);
        plan.setHours(hrs);
        planService.updatePlan(plan);

        mark.setSemester(sem);
        mark.setControlMethod(controlMethodService.getControlMethodByName(ctrl));
        mark.setFinalGrade(grade);
        marksService.saveMark(mark);
        updateGrid();
    }

    private void enterSyncMode() {
        String group = groupSelect.getValue();
        String current = studentSelect.getValue();
        if (group == null || current == null) {
            return;
        }
        otherStudentSelect.setItems(groupService.getAllStudentsForSelectedGroup(group)
                .stream()
                .filter(s -> !s.equals(current))
                .sorted(ukrainianCollator)
                .toList());
        studentSelect.setEnabled(false);
        syncButton.setVisible(false);
        otherStudentSelect.setVisible(true);
        applySyncButton.setVisible(true);
        cancelSyncButton.setVisible(true);

        grid.setVisible(false);
        grid.setEnabled(false);

    }

    private void exitSyncMode() {

        otherStudentSelect.clear();
        otherStudentSelect.setVisible(false);
        otherGrid.setVisible(false);
        studentSelect.setEnabled(true);
        syncButton.setVisible(true);
        applySyncButton.setVisible(false);
        cancelSyncButton.setVisible(false);
        grid.setEnabled(true);
        grid.setVisible(true);
    }

    private void updateOtherGrid() {
        String otherName = otherStudentSelect.getValue();
        String group = groupSelect.getValue();
        if (otherName == null || group == null) {
            otherGrid.setItems(List.of());
            otherGrid.setVisible(false);
            return;
        }
        StudentEntity student = studentService.getStudentForCard(group, otherName);
        ensureZeroMarks(student);
        List<Row> rows = marksService.getMarksByStudent(student).stream()
                .map(m -> new Row(
                        null,
                        m.getPlan().getDiscipline().getTitle(),
                        m.getSemester(),
                        m.getPlan().getHours(),
                        m.getControlMethod().getName(),
                        0))
                .toList();
        otherGrid.setItems(rows);
        otherGrid.setVisible(true);
    }


    private void ensureZeroMarks(StudentEntity student) {
        List<StudentPlansEntity> mappings = studentPlansService.getPlansForStudent(student);
        for (StudentPlansEntity sp : mappings) {
            marksInitializerService.initializeMarksForPlan(sp.getPlan(), List.of(student));
        }
    }

    private void performSync() {
        String otherName = otherStudentSelect.getValue();
        String group = groupSelect.getValue();
        String currentName = studentSelect.getValue();
        if (otherName == null || group == null || currentName == null) {
            return;
        }
        StudentEntity target = studentService.getStudentForCard(group, currentName);
        StudentEntity source = studentService.getStudentForCard(group, otherName);
        syncService.synchronize(target, source);
        ensureZeroMarks(target);
        exitSyncMode();
        updateGrid();
    }

    /** Row DTO for grid. */
    private record Row(Long id, String discipline, int semester, int hours, String controlType, int grade) {
    }
}