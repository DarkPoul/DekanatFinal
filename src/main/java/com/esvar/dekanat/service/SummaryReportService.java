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
        if (selectedGroup == null) {
            throw new SummaryReportGenerationException("Оберіть групу для формування звіту");
        }

        StudentGroupEntity group = Optional.ofNullable(groupService.getGroupByTitle(selectedGroup.getGroupCode()))
                .orElseThrow(() -> new SummaryReportGenerationException("Групу не знайдено"));

        List<StudentEntity> students = sortStudentsByFullName(studentService.getStudentByGroupId(group.getId()));
        if (students.isEmpty()) {
            throw new SummaryReportGenerationException("У групі немає студентів");
        }

        int semester = computeFirstModuleSemester(selectedGroup.getCourse());
        Map<Long, PlanAssignment> planAssignments = collectPlanAssignments(group, semester, students);
        if (planAssignments.isEmpty()) {
            throw new SummaryReportGenerationException("Не знайдено дисциплін для першого модульного контролю");
        }

        List<String> studentFullNames = students.stream()
                .map(StudentEntity::getFullName)
                .collect(Collectors.toList());

        List<DisciplineSummary> disciplineSummaries = buildDisciplineSummaries(planAssignments.values(), students);
        if (disciplineSummaries.isEmpty()) {
            throw new SummaryReportGenerationException("Список дисциплін порожній");
        }

        List<SummaryReportPdfGenerator.DisciplineColumn> disciplineColumns = disciplineSummaries.stream()
                .map(summary -> new SummaryReportPdfGenerator.DisciplineColumn(summary.title(), summary.elective()))
                .collect(Collectors.toList());

        Map<String, List<Integer>> marksByStudent = buildMarksForSummaryReport(students, disciplineSummaries);
        byte[] pdfBytes = summaryReportPdfGenerator.generateSummaryReport(
                group.getGroupCode(),
                studentFullNames,
                disciplineColumns,
                marksByStudent,
                true
        );

        if (pdfBytes == null || pdfBytes.length == 0) {
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

        List<PlansEntity> groupPlans = planService.getAllPlansForGroupAndSemester(group, semester);
        if (groupPlans != null) {
            groupPlans.stream()
                    .filter(this::isFirstModulePlan)
                    .forEach(plan -> assignments.computeIfAbsent(plan.getId(), id -> new PlanAssignment(plan)));
        }

        for (StudentEntity student : students) {
            List<StudentPlansEntity> studentPlans = studentPlansService.getPlansForStudent(student);
            if (studentPlans == null || studentPlans.isEmpty()) {
                continue;
            }

            for (StudentPlansEntity studentPlan : studentPlans) {
                PlansEntity plan = studentPlan.getPlan();
                if (plan == null || plan.getSemester() != semester || !isFirstModulePlan(plan)) {
                    continue;
                }

                assignments.computeIfAbsent(plan.getId(), id -> new PlanAssignment(plan))
                        .assignedStudentIds.add(student.getId());
            }
        }

        return assignments;
    }

    private boolean isFirstModulePlan(PlansEntity plan) {
        if (plan == null) {
            return false;
        }
        return isFirstModuleControl(plan.getFirstControl());
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
        return normalizedLowerCase.contains(FIRST_MODULE_KEYWORD);
    }

    private Map<String, List<Integer>> buildMarksForSummaryReport(List<StudentEntity> students,
                                                                 List<DisciplineSummary> disciplineSummaries) {
        List<String> studentNames = students.stream()
                .map(StudentEntity::getFullName)
                .collect(Collectors.toList());

        Map<String, List<Integer>> marksByStudent = new LinkedHashMap<>();
        for (String studentName : studentNames) {
            marksByStudent.put(studentName, new ArrayList<>());
        }

        for (DisciplineSummary disciplineSummary : disciplineSummaries) {
            PlansEntity plan = disciplineSummary.plan();
            ControlMethodEntity firstControl = plan.getFirstControl();
            List<MarksEntity> marks = marksService.findMarksByPlanAndControlMethod(plan, firstControl);
            if (marks == null) {
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
                    marksByStudent.get(studentName).add(null);
                    continue;
                }

                int markValue = marksByStudentId.getOrDefault(student.getId(), 0);
                marksByStudent.get(studentName).add(markValue);
            }
        }

        return marksByStudent;
    }

    private List<DisciplineSummary> buildDisciplineSummaries(Collection<PlanAssignment> assignments,
                                                             List<StudentEntity> students) {
        if (assignments == null || assignments.isEmpty()) {
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
                continue;
            }

            String title = discipline.getTitle();
            if (title == null) {
                continue;
            }

            title = title.trim();
            if (title.isBlank()) {
                continue;
            }

            Set<Long> assignedStudentIds = new LinkedHashSet<>(assignment.assignedStudentIds);
            if (assignedStudentIds.isEmpty() && !plan.isElective()) {
                assignedStudentIds.addAll(groupStudentIds);
            } else if (!assignedStudentIds.isEmpty()) {
                assignedStudentIds.retainAll(groupStudentIds);
            }

            if (assignedStudentIds.isEmpty()) {
                continue;
            }

            boolean matchesGroup = assignedStudentIds.containsAll(groupStudentIds)
                    && groupStudentIds.containsAll(assignedStudentIds);
            boolean elective = plan.isElective() || !matchesGroup;

            summaries.add(new DisciplineSummary(plan, title, elective, assignedStudentIds));
        }

        return summaries.stream()
                .sorted(Comparator.comparing(DisciplineSummary::title, ukrainianCollator))
                .collect(Collectors.toList());
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
