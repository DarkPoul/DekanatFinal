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
        List<PlansEntity> plans = collectPlansForSummaryReport(group, semester, students);
        if (plans.isEmpty()) {
            throw new SummaryReportGenerationException("Не знайдено дисциплін для першого модульного контролю");
        }

        List<String> studentFullNames = students.stream()
                .map(StudentEntity::getFullName)
                .collect(Collectors.toList());

        List<DisciplineSummary> disciplineSummaries = buildDisciplineSummaries(plans, students);
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

    private List<PlansEntity> collectPlansForSummaryReport(StudentGroupEntity group,
                                                           int semester,
                                                           List<StudentEntity> students) {
        Map<Long, PlansEntity> uniquePlans = new LinkedHashMap<>();

        List<PlansEntity> groupPlans = planService.getAllPlansForGroupAndSemester(group, semester);
        if (groupPlans != null) {
            groupPlans.stream()
                    .filter(this::isFirstModulePlan)
                    .forEach(plan -> uniquePlans.putIfAbsent(plan.getId(), plan));
        }

        for (StudentEntity student : students) {
            studentPlansService.getPlansForStudent(student).stream()
                    .map(StudentPlansEntity::getPlan)
                    .filter(Objects::nonNull)
                    .filter(plan -> plan.getSemester() == semester)
                    .filter(this::isFirstModulePlan)
                    .forEach(plan -> uniquePlans.putIfAbsent(plan.getId(), plan));
        }

        return uniquePlans.values().stream()
                .sorted(Comparator.comparing(
                        plan -> Optional.ofNullable(plan.getDiscipline())
                                .map(DisciplineEntity::getTitle)
                                .orElse("")
                        , ukrainianCollator))
                .collect(Collectors.toList());
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

    private List<DisciplineSummary> buildDisciplineSummaries(List<PlansEntity> plans, List<StudentEntity> students) {
        if (plans == null || plans.isEmpty()) {
            return List.of();
        }

        Set<Long> groupStudentIds = students.stream()
                .map(StudentEntity::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<DisciplineSummary> summaries = new ArrayList<>();
        for (PlansEntity plan : plans) {
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

            Set<Long> assignedStudentIds = studentPlansService.getStudentByPlan(plan).stream()
                    .map(StudentEntity::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (assignedStudentIds.isEmpty() && !plan.isElective()) {
                assignedStudentIds.addAll(groupStudentIds);
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

    public static class SummaryReportGenerationException extends RuntimeException {
        public SummaryReportGenerationException(String message) {
            super(message);
        }
    }
}
