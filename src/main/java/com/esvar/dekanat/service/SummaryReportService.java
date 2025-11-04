package com.esvar.dekanat.service;

import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.entity.ControlMethodEntity;
import com.esvar.dekanat.entity.DisciplineEntity;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import com.esvar.dekanat.entity.StudentPlansEntity;
import com.esvar.dekanat.generate.summary.SummaryReportPdfGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SummaryReportService {

    private static final String CONTROL_TYPE_FIRST_MODULE = "Перший модульний контроль";
    private static final String FIRST_MODULE_KEYWORD = "перший модуль";

    private final GroupService groupService;
    private final StudentService studentService;
    private final StudentPlansService studentPlansService;
    private final PlanService planService;
    private final MarksService marksService;
    private final SummaryReportPdfGenerator summaryReportPdfGenerator;
    private final Collator ukrainianCollator = Collator.getInstance(new Locale("uk", "UA"));

    public SummaryReportService(GroupService groupService,
                                StudentService studentService,
                                StudentPlansService studentPlansService,
                                PlanService planService,
                                MarksService marksService,
                                SummaryReportPdfGenerator summaryReportPdfGenerator) {
        this.groupService = groupService;
        this.studentService = studentService;
        this.studentPlansService = studentPlansService;
        this.planService = planService;
        this.marksService = marksService;
        this.summaryReportPdfGenerator = summaryReportPdfGenerator;
    }

    @Transactional(readOnly = true)
    public SummaryReportResult generateFirstModuleReport(GroupDTO selectedGroup) {
        System.out.println("[SummaryReportService] Початок генерації звіту для першого модулю");
        if (selectedGroup == null) {
            System.out.println("[SummaryReportService] Не обрано групу для звіту");
            throw new SummaryReportGenerationException("Оберіть групу для формування звіту");
        }

        System.out.println("[SummaryReportService] Обрано групу: " + selectedGroup.getGroupCode());
        StudentGroupEntity group = Optional.ofNullable(groupService.getGroupByTitle(selectedGroup.getGroupCode()))
                .orElseThrow(() -> new SummaryReportGenerationException("Групу не знайдено"));

        System.out.println("[SummaryReportService] Завантажуємо студентів групи");
        List<StudentEntity> students = sortStudentsByFullName(studentService.getStudentByGroupId(group.getId()));
        if (students.isEmpty()) {
            System.out.println("[SummaryReportService] У групі не знайдено студентів");
            throw new SummaryReportGenerationException("У групі немає студентів");
        }

        System.out.println("[SummaryReportService] Знайдено студентів: " + students.size());

        int semester = computeFirstModuleSemester(selectedGroup.getCourse());
        System.out.println("[SummaryReportService] Обчислено семестр першого модулю: " + semester);
        Map<Long, PlanAssignment> planAssignments = collectPlanAssignments(group, semester, students);
        if (planAssignments.isEmpty()) {
            System.out.println("[SummaryReportService] Не знайдено планів для першого модулю");
            throw new SummaryReportGenerationException("Не знайдено дисциплін для першого модульного контролю");
        }

        System.out.println("[SummaryReportService] Зібрано планів: " + planAssignments.size());

        List<String> studentFullNames = students.stream()
                .map(StudentEntity::getFullName)
                .collect(Collectors.toList());

        List<DisciplineSummary> disciplineSummaries = buildDisciplineSummaries(planAssignments.values(), students);
        if (disciplineSummaries.isEmpty()) {
            System.out.println("[SummaryReportService] Не сформовано жодної дисципліни");
            throw new SummaryReportGenerationException("Список дисциплін порожній");
        }

        System.out.println("[SummaryReportService] Сформовано дисциплін: " + disciplineSummaries.size());

        List<SummaryReportPdfGenerator.DisciplineColumn> disciplineColumns = disciplineSummaries.stream()
                .map(summary -> new SummaryReportPdfGenerator.DisciplineColumn(summary.title(), summary.elective()))
                .collect(Collectors.toList());

        System.out.println("[SummaryReportService] Формуємо оцінки по кожному студенту");
        Map<String, List<Integer>> marksByStudent = buildMarksForSummaryReport(students, disciplineSummaries);
        System.out.println("[SummaryReportService] Отримано записи оцінок: " + marksByStudent.size());
        byte[] pdfBytes = summaryReportPdfGenerator.generateSummaryReport(
                group.getGroupCode(),
                studentFullNames,
                disciplineColumns,
                marksByStudent,
                true
        );

        System.out.println("[SummaryReportService] Генерація PDF завершена, розмір: " + (pdfBytes == null ? 0 : pdfBytes.length));
        if (pdfBytes == null || pdfBytes.length == 0) {
            System.out.println("[SummaryReportService] Отримано порожній PDF");
            throw new SummaryReportGenerationException("Не вдалося сформувати звіт");
        }

        return new SummaryReportResult(group.getGroupCode(), pdfBytes);
    }

    private List<StudentEntity> sortStudentsByFullName(List<StudentEntity> students) {
        return students.stream()
                .sorted(Comparator.comparing(StudentEntity::getFullName, ukrainianCollator))
                .collect(Collectors.toList());
    }

    private int computeFirstModuleSemester(int course) {
        if (course <= 0) {
            return 1;
        }
        return course * 2 - 1;
    }

    private Map<Long, PlanAssignment> collectPlanAssignments(StudentGroupEntity group,
                                                             int semester,
                                                             List<StudentEntity> students) {
        Map<Long, PlanAssignment> assignments = new LinkedHashMap<>();

        System.out.println("[SummaryReportService] Завантажуємо плани групи для семестру " + semester);
        List<PlansEntity> groupPlans = planService.getAllPlansForGroupAndSemester(group, semester);
        if (groupPlans != null) {
            System.out.println("[SummaryReportService] Отримано планів групи: " + groupPlans.size());
            groupPlans.stream()
                    .filter(this::isFirstModulePlan)
                    .forEach(plan -> {
                        System.out.println("[SummaryReportService] Додаємо план групи: " + safeDisciplineTitle(plan));
                        assignments.computeIfAbsent(plan.getId(), id -> new PlanAssignment(plan));
                    });
        }

        for (StudentEntity student : students) {
            System.out.println("[SummaryReportService] Обробка студентського плану: " + student.getFullName());
            List<StudentPlansEntity> studentPlans = studentPlansService.getPlansForStudent(student);
            if (studentPlans == null || studentPlans.isEmpty()) {
                System.out.println("[SummaryReportService] Для студента немає індивідуальних планів");
                continue;
            }

            for (StudentPlansEntity studentPlan : studentPlans) {
                PlansEntity plan = studentPlan.getPlan();
                if (plan == null || plan.getSemester() != semester || !isFirstModulePlan(plan)) {
                    System.out.println("[SummaryReportService] Пропускаємо план (не підходить для 1 модулю)");
                    continue;
                }

                assignments.computeIfAbsent(plan.getId(), id -> {
                            System.out.println("[SummaryReportService] Додаємо індивідуальний план: " + safeDisciplineTitle(plan));
                            return new PlanAssignment(plan);
                        })
                        .assignedStudentIds.add(student.getId());
                System.out.println("[SummaryReportService] Призначено студента до плану");
            }
        }

        System.out.println("[SummaryReportService] Повертаємо зібрані призначення планів: " + assignments.size());
        return assignments;
    }

    private boolean isFirstModulePlan(PlansEntity plan) {
        if (plan == null) {
            return false;
        }
        boolean result = isFirstModuleControl(plan.getFirstControl());
        System.out.println("[SummaryReportService] План '" + safeDisciplineTitle(plan) + "' "
                + (result ? "відноситься" : "не відноситься") + " до першого модулю");
        return result;
    }

    private boolean isFirstModuleControl(ControlMethodEntity controlMethod) {
        if (controlMethod == null) {
            return false;
        }
        String controlName = controlMethod.getName();
        if (controlName == null) {
            return false;
        }

        String normalizedName = controlName.trim();
        if (normalizedName.equalsIgnoreCase(CONTROL_TYPE_FIRST_MODULE)) {
            return true;
        }

        String normalizedLowerCase = normalizedName.toLowerCase(Locale.ROOT);
        boolean result = normalizedLowerCase.contains(FIRST_MODULE_KEYWORD);
        System.out.println("[SummaryReportService] Контроль '" + normalizedName + "' містить ключове слово першого модулю: " + result);
        return result;
    }

    private Map<String, List<Integer>> buildMarksForSummaryReport(List<StudentEntity> students,
                                                                 List<DisciplineSummary> disciplineSummaries) {
        System.out.println("[SummaryReportService] Початок формування оцінок");
        List<String> studentNames = students.stream()
                .map(StudentEntity::getFullName)
                .collect(Collectors.toList());

        Map<String, List<Integer>> marksByStudent = new LinkedHashMap<>();
        for (String studentName : studentNames) {
            marksByStudent.put(studentName, new ArrayList<>());
        }

        for (DisciplineSummary disciplineSummary : disciplineSummaries) {
            System.out.println("[SummaryReportService] Обробка дисципліни при формуванні оцінок: " + disciplineSummary.title());
            PlansEntity plan = disciplineSummary.plan();
            ControlMethodEntity firstControl = plan.getFirstControl();
            List<MarksEntity> marks = marksService.findMarksByPlanAndControlMethod(plan, firstControl);
            if (marks == null) {
                System.out.println("[SummaryReportService] Для дисципліни немає оцінок");
                marks = Collections.emptyList();
            }

            Map<Long, Integer> marksByStudentId = marks.stream()
                    .collect(Collectors.toMap(
                            mark -> mark.getStudent().getId(),
                            MarksEntity::getFinalGrade,
                            (existing, replacement) -> replacement
                    ));

            for (int i = 0; i < students.size(); i++) {
                StudentEntity student = students.get(i);
                String studentName = studentNames.get(i);

                if (!disciplineSummary.assignedStudentIds().contains(student.getId())) {
                    System.out.println("[SummaryReportService] Студент " + studentName + " не відвідує дисципліну " + disciplineSummary.title());
                    marksByStudent.get(studentName).add(null);
                    continue;
                }

                int markValue = marksByStudentId.getOrDefault(student.getId(), 0);
                System.out.println("[SummaryReportService] Додаємо оцінку " + markValue + " студенту " + studentName);
                marksByStudent.get(studentName).add(markValue);
            }
        }

        System.out.println("[SummaryReportService] Формування оцінок завершено");
        return marksByStudent;
    }

    private List<DisciplineSummary> buildDisciplineSummaries(Collection<PlanAssignment> assignments,
                                                             List<StudentEntity> students) {
        System.out.println("[SummaryReportService] Формуємо підсумок по дисциплінах");
        if (assignments == null || assignments.isEmpty()) {
            System.out.println("[SummaryReportService] Список призначень порожній");
            return List.of();
        }

        Set<Long> groupStudentIds = students.stream()
                .map(StudentEntity::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<DisciplineSummary> summaries = new ArrayList<>();
        for (PlanAssignment assignment : assignments) {
            PlansEntity plan = assignment.plan;
            DisciplineEntity discipline = plan.getDiscipline();
            if (discipline == null) {
                System.out.println("[SummaryReportService] Пропущено план без дисципліни");
                continue;
            }

            String title = discipline.getTitle();
            if (title == null) {
                System.out.println("[SummaryReportService] Пропущено дисципліну без назви");
                continue;
            }

            title = title.trim();
            if (title.isBlank()) {
                System.out.println("[SummaryReportService] Пропущено дисципліну з порожньою назвою");
                continue;
            }

            Set<Long> assignedStudentIds = new LinkedHashSet<>(assignment.assignedStudentIds);
            if (assignedStudentIds.isEmpty() && !plan.isElective()) {
                System.out.println("[SummaryReportService] План обов'язковий, додаємо всіх студентів");
                assignedStudentIds.addAll(groupStudentIds);
            } else if (!assignedStudentIds.isEmpty()) {
                assignedStudentIds.retainAll(groupStudentIds);
            }

            if (assignedStudentIds.isEmpty()) {
                System.out.println("[SummaryReportService] Пропущено дисципліну без студентів");
                continue;
            }

            boolean matchesGroup = assignedStudentIds.containsAll(groupStudentIds)
                    && groupStudentIds.containsAll(assignedStudentIds);
            boolean elective = plan.isElective() || !matchesGroup;

            System.out.println("[SummaryReportService] Додаємо дисципліну: " + title + ", вибіркова=" + elective);
            summaries.add(new DisciplineSummary(plan, title, elective, assignedStudentIds));
        }

        List<DisciplineSummary> sorted = summaries.stream()
                .sorted(Comparator.comparing(DisciplineSummary::title, ukrainianCollator))
                .collect(Collectors.toList());
        System.out.println("[SummaryReportService] Підсумок дисциплін сформовано: " + sorted.size());
        return sorted;
    }

    private String safeDisciplineTitle(PlansEntity plan) {
        DisciplineEntity discipline = plan.getDiscipline();
        if (discipline == null || discipline.getTitle() == null) {
            return "<без назви>";
        }
        return discipline.getTitle();
    }

    public record SummaryReportResult(String groupCode, byte[] pdfBytes) {
    }

    private record DisciplineSummary(PlansEntity plan, String title, boolean elective, Set<Long> assignedStudentIds) {
    }

    private static class PlanAssignment {
        private final PlansEntity plan;
        private final Set<Long> assignedStudentIds = new LinkedHashSet<>();

        private PlanAssignment(PlansEntity plan) {
            this.plan = plan;
        }
    }

    public static class SummaryReportGenerationException extends RuntimeException {
        public SummaryReportGenerationException(String message) {
            super(message);
        }
    }
}
