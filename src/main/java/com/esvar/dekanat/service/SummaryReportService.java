package com.esvar.dekanat.service;

import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.entity.ControlMethodEntity;
import com.esvar.dekanat.entity.DisciplineEntity;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.SpecialtyEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import com.esvar.dekanat.entity.StudentPlansEntity;
import com.esvar.dekanat.entity.EduProgramEntity;
import com.esvar.dekanat.generate.summary.SummaryReportPdfGenerator;
import com.esvar.dekanat.security.SecurityService;
import com.esvar.dekanat.user.UserModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SummaryReportService {

    private static final String CONTROL_TYPE_FIRST_MODULE = "Перший модульний контроль";
    private static final String CONTROL_TYPE_SECOND_MODULE = "Другий модульний контроль";
    private static final String CONTROL_TYPE_TWO_MODULES = "Два модульних контроля";
    private static final String CONTROL_TYPE_SEMESTER = "Семестровий контроль";
    private static final List<String> SEMESTER_CONTROL_TYPES = List.of(
            CONTROL_TYPE_SEMESTER,
            "Семестровий",
            "Залік",
            "Екзамен",
            "Диференційний залік",
            "Курсова робота",
            "Курсовий проєкт",
            "РР(фінальна оцінка)",
            "РГР(фінальна оцінка)",
            "Контрольна робота"
    );

    private final GroupService groupService;
    private final StudentService studentService;
    private final StudentPlansService studentPlansService;
    private final PlanService planService;
    private final MarksService marksService;
    private final SummaryReportPdfGenerator summaryReportPdfGenerator;
    private final SecurityService securityService;
    private final Collator ukrainianCollator = Collator.getInstance(new Locale("uk", "UA"));

    public SummaryReportService(GroupService groupService,
                                StudentService studentService,
                                StudentPlansService studentPlansService,
                                PlanService planService,
                                MarksService marksService,
                                SummaryReportPdfGenerator summaryReportPdfGenerator,
                                SecurityService securityService) {
        this.groupService = groupService;
        this.studentService = studentService;
        this.studentPlansService = studentPlansService;
        this.planService = planService;
        this.marksService = marksService;
        this.summaryReportPdfGenerator = summaryReportPdfGenerator;
        this.securityService = securityService;
    }

    @Transactional(readOnly = true)
    public SummaryReportResult generateFirstModuleReport(GroupDTO selectedGroup, boolean isWinterSession) {
        return generateReport(selectedGroup, List.of(CONTROL_TYPE_FIRST_MODULE), CONTROL_TYPE_FIRST_MODULE, true, isWinterSession);
    }

    @Transactional(readOnly = true)
    public SummaryReportResult generateSecondModuleReport(GroupDTO selectedGroup, boolean isWinterSession) {
        return generateTwoModuleReport(selectedGroup, isWinterSession);
    }

    @Transactional(readOnly = true)
    public SummaryReportResult generateSemesterReport(GroupDTO selectedGroup, boolean isWinterSession) {
        return generateReport(selectedGroup, SEMESTER_CONTROL_TYPES, CONTROL_TYPE_SEMESTER, true, isWinterSession);
    }

    private SummaryReportResult generateTwoModuleReport(GroupDTO selectedGroup, boolean isWinterSession) {
        System.out.println("[SummaryReportService] Початок генерації звіту за два модулі");
        if (selectedGroup == null) {
            throw new SummaryReportGenerationException("Оберіть групу для формування звіту");
        }

        StudentGroupEntity group = Optional.ofNullable(groupService.getGroupByTitle(selectedGroup.getGroupCode()))
                .orElseThrow(() -> new SummaryReportGenerationException("Групу не знайдено"));

        List<StudentEntity> students = sortStudentsByFullName(studentService.getStudentByGroupId(group.getId()));
        if (students.isEmpty()) {
            throw new SummaryReportGenerationException("У групі немає студентів");
        }

        int semester = computeFirstModuleSemester(selectedGroup.getCourse(), isWinterSession);
        Map<Long, TwoModulePlanAssignment> planAssignments = collectTwoModuleAssignments(group, semester, students);
        if (planAssignments.isEmpty()) {
            throw new SummaryReportGenerationException("Не знайдено дисциплін для обраного контролю");
        }

        List<String> studentFullNames = students.stream()
                .map(StudentEntity::getFullName)
                .collect(Collectors.toList());

        List<TwoModuleDisciplineSummary> disciplineSummaries = buildTwoModuleDisciplineSummaries(planAssignments.values(), students);
        if (disciplineSummaries.isEmpty()) {
            throw new SummaryReportGenerationException("Список дисциплін порожній");
        }

        List<SummaryReportPdfGenerator.DisciplineColumn> disciplineColumns = disciplineSummaries.stream()
                .map(summary -> new SummaryReportPdfGenerator.DisciplineColumn(summary.title(), summary.elective()))
                .collect(Collectors.toList());

        Map<String, List<SummaryReportPdfGenerator.ModuleMark>> marksByStudent =
                buildTwoModuleMarksForSummaryReport(students, disciplineSummaries);

        String examiner = resolveCurrentTeacherName();

        byte[] pdfBytes = summaryReportPdfGenerator.generateTwoModuleSummaryReport(
                group.getGroupCode(),
                studentFullNames,
                disciplineColumns,
                marksByStudent,
                true,
                examiner,
                false,
                true,
                buildReportTitle(CONTROL_TYPE_TWO_MODULES)
        );

        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new SummaryReportGenerationException("Не вдалося сформувати звіт");
        }

        return new SummaryReportResult(group.getGroupCode(), pdfBytes);
    }

    private SummaryReportResult generateReport(GroupDTO selectedGroup,
                                               Collection<String> controlTypes,
                                               String controlTitle,
                                               boolean includeSignature,
                                               boolean isWinterSession) {
        System.out.println("[SummaryReportService] Початок генерації звіту для контролю: " + controlTitle);
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

        int semester = computeSemesterForControl(selectedGroup.getCourse(), controlTitle, isWinterSession);
        System.out.println("[SummaryReportService] Обчислено семестр для контролю: " + semester);
        Map<Long, PlanAssignment> planAssignments = collectPlanAssignments(group, semester, students, controlTypes);
        if (planAssignments.isEmpty()) {
            System.out.println("[SummaryReportService] Не знайдено планів для контролю: " + controlTitle);
            throw new SummaryReportGenerationException("Не знайдено дисциплін для обраного контролю");
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
        String examiner = resolveCurrentTeacherName();

        byte[] pdfBytes = summaryReportPdfGenerator.generateSummaryReport(
                group.getGroupCode(),
                studentFullNames,
                disciplineColumns,
                marksByStudent,
                true,
                examiner,
                false,
                includeSignature,
                buildReportTitle(controlTitle)
        );

        System.out.println("[SummaryReportService] Генерація PDF завершена, розмір: " + (pdfBytes == null ? 0 : pdfBytes.length));
        if (pdfBytes == null || pdfBytes.length == 0) {
            System.out.println("[SummaryReportService] Отримано порожній PDF");
            throw new SummaryReportGenerationException("Не вдалося сформувати звіт");
        }

        return new SummaryReportResult(group.getGroupCode(), pdfBytes);
    }

    private String resolveCurrentTeacherName() {
        return securityService.getCurrentUserModel()
                .map(this::formatTeacherName)
                .orElse("");
    }

    private int computeSecondModuleSemester(int course, boolean isWinterSession) {
        return isWinterSession ? course * 2 - 1 : course * 2;
    }

    private int computeSemesterForControl(int course, String controlTitle, boolean isWinterSession) {
        if (CONTROL_TYPE_SECOND_MODULE.equals(controlTitle) || CONTROL_TYPE_SEMESTER.equals(controlTitle)) {
            return computeSecondModuleSemester(course, isWinterSession);
        }
        return computeFirstModuleSemester(course, isWinterSession);
    }

    private String formatTeacherName(UserModel user) {
        String lastName = capitalize(user.getLastname());
        String firstName = capitalize(user.getFirstname());
        String patronymic = capitalize(user.getPatronymic());
        return Arrays.stream(new String[]{lastName, firstName, patronymic})
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String[] parts = value.split("-");
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                parts[i] = parts[i].substring(0, 1).toUpperCase(Locale.ROOT)
                        + parts[i].substring(1).toLowerCase(Locale.ROOT);
            }
        }
        return String.join("-", parts);
    }

    private List<StudentEntity> sortStudentsByFullName(List<StudentEntity> students) {
        return students.stream()
                .sorted(Comparator.comparing(StudentEntity::getFullName, ukrainianCollator))
                .collect(Collectors.toList());
    }

    private int computeFirstModuleSemester(int course, boolean isWinterSession) {
        if (course <= 0) {
            return 1;
        }
        return isWinterSession ? course * 2 - 1 : course * 2;
    }

    private String buildReportTitle(String controlType) {
        if (controlType == null || controlType.isBlank()) {
            return "";
        }
        return "за " + controlType.toLowerCase(Locale.ROOT);
    }

    private Map<Long, PlanAssignment> collectPlanAssignments(StudentGroupEntity group,
                                                            int semester,
                                                            List<StudentEntity> students,
                                                            Collection<String> controlTypes) {
        Map<Long, PlanAssignment> assignments = new LinkedHashMap<>();
        Map<Long, ControlMethodEntity> controlMethodCache = new HashMap<>();

        System.out.println("[SummaryReportService] Завантажуємо плани групи для семестру " + semester);
        List<PlansEntity> groupPlans = planService.getAllPlansForGroupAndSemester(group, semester);
        if (groupPlans != null) {
            System.out.println("[SummaryReportService] Отримано планів групи: " + groupPlans.size());
            for (PlansEntity plan : groupPlans) {
                PlanAssignment assignment = ensurePlanAssignment(assignments, plan, controlMethodCache, controlTypes);
                if (assignment != null) {
                    System.out.println("[SummaryReportService] Додаємо план групи: " + safeDisciplineTitle(plan));
                }
            }
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
                if (plan == null || plan.getSemester() != semester) {
                    System.out.println("[SummaryReportService] Пропускаємо план (не підходить для обраного контролю)");
                    continue;
                }

                if (!isPlanFromSameProgram(plan, group)) {
                    System.out.println("[SummaryReportService] Пропускаємо план з іншої освітньої програми");
                    continue;
                }

                PlanAssignment assignment = ensurePlanAssignment(assignments, plan, controlMethodCache, controlTypes);
                if (assignment == null) {
                    System.out.println("[SummaryReportService] Пропускаємо план (не підходить для обраного контролю)");
                    continue;
                }

                assignment.assignedStudentIds.add(student.getId());
                System.out.println("[SummaryReportService] Призначено студента до плану");
            }
        }

        System.out.println("[SummaryReportService] Повертаємо зібрані призначення планів: " + assignments.size());
        return assignments;
    }

    private Map<Long, TwoModulePlanAssignment> collectTwoModuleAssignments(StudentGroupEntity group,
                                                                           int semester,
                                                                           List<StudentEntity> students) {
        Map<Long, TwoModulePlanAssignment> assignments = new LinkedHashMap<>();
        Map<Long, ControlMethodEntity> firstControlCache = new HashMap<>();
        Map<Long, ControlMethodEntity> secondControlCache = new HashMap<>();

        System.out.println("[SummaryReportService] Завантажуємо плани групи для двох модулів, семестр " + semester);
        List<PlansEntity> groupPlans = planService.getAllPlansForGroupAndSemester(group, semester);
        if (groupPlans != null) {
            for (PlansEntity plan : groupPlans) {
                TwoModulePlanAssignment assignment = ensureTwoModulePlanAssignment(
                        assignments,
                        plan,
                        firstControlCache,
                        secondControlCache
                );
                if (assignment != null) {
                    System.out.println("[SummaryReportService] Додаємо план групи (2 модулі): " + safeDisciplineTitle(plan));
                }
            }
        }

        for (StudentEntity student : students) {
            List<StudentPlansEntity> studentPlans = studentPlansService.getPlansForStudent(student);
            if (studentPlans == null || studentPlans.isEmpty()) {
                continue;
            }

            for (StudentPlansEntity studentPlan : studentPlans) {
                PlansEntity plan = studentPlan.getPlan();
                if (plan == null || plan.getSemester() != semester) {
                    continue;
                }

                if (!isPlanFromSameProgram(plan, group)) {
                    System.out.println("[SummaryReportService] Пропускаємо план з іншої освітньої програми (2 модулі)");
                    continue;
                }

                TwoModulePlanAssignment assignment = ensureTwoModulePlanAssignment(
                        assignments,
                        plan,
                        firstControlCache,
                        secondControlCache
                );
                if (assignment == null) {
                    continue;
                }

                assignment.assignedStudentIds.add(student.getId());
            }
        }

        System.out.println("[SummaryReportService] Повертаємо зібрані призначення планів (2 модулі): " + assignments.size());
        return assignments;
    }

    private PlanAssignment ensurePlanAssignment(Map<Long, PlanAssignment> assignments,
                                                PlansEntity plan,
                                                Map<Long, ControlMethodEntity> controlCache,
                                                Collection<String> controlTypes) {
        if (plan == null) {
            return null;
        }

        ControlMethodEntity control = resolveControlMethod(plan, controlCache, controlTypes);
        if (control == null) {
            System.out.println("[SummaryReportService] План '" + safeDisciplineTitle(plan) + "' не має контрольної події зі списку '" + formatControlTypes(controlTypes) + "'");
            return null;
        }

        PlanAssignment assignment = assignments.get(plan.getId());
        if (assignment == null) {
            assignment = new PlanAssignment(plan, control);
            assignments.put(plan.getId(), assignment);
        }
        return assignment;
    }

    private TwoModulePlanAssignment ensureTwoModulePlanAssignment(Map<Long, TwoModulePlanAssignment> assignments,
                                                                  PlansEntity plan,
                                                                  Map<Long, ControlMethodEntity> firstControlCache,
                                                                  Map<Long, ControlMethodEntity> secondControlCache) {
        if (plan == null) {
            return null;
        }

        ControlMethodEntity firstControl = resolveControlMethod(plan, firstControlCache, List.of(CONTROL_TYPE_FIRST_MODULE));
        ControlMethodEntity secondControl = resolveControlMethod(plan, secondControlCache, List.of(CONTROL_TYPE_SECOND_MODULE));

        if (firstControl == null && secondControl == null) {
            System.out.println("[SummaryReportService] План '" + safeDisciplineTitle(plan)
                    + "' не має модульних контролів");
            return null;
        }

        TwoModulePlanAssignment assignment = assignments.get(plan.getId());
        if (assignment == null) {
            assignment = new TwoModulePlanAssignment(plan, firstControl, secondControl);
            assignments.put(plan.getId(), assignment);
        }
        return assignment;
    }

    private ControlMethodEntity resolveControlMethod(PlansEntity plan,
                                                     Map<Long, ControlMethodEntity> controlCache,
                                                     Collection<String> controlTypes) {
        if (plan == null || plan.getId() == null) {
            return null;
        }

        if (controlCache.containsKey(plan.getId())) {
            return controlCache.get(plan.getId());
        }

        ControlMethodEntity control = findControlMethodInPlan(plan, controlTypes);
        if (control == null) {
            control = findControlMethodByMarks(plan, controlTypes);
        }

        controlCache.put(plan.getId(), control);
        System.out.println("[SummaryReportService] План '" + safeDisciplineTitle(plan) + "' "
                + (control != null ? "має" : "не має") + " контроль зі списку '" + formatControlTypes(controlTypes) + "'");
        return control;
    }

    private ControlMethodEntity findControlMethodInPlan(PlansEntity plan, Collection<String> controlTypes) {
        for (String controlType : controlTypes) {
            if (isMatchingControl(plan.getFirstControl(), controlType)) {
                return plan.getFirstControl();
            }
            if (isMatchingControl(plan.getSecondControl(), controlType)) {
                return plan.getSecondControl();
            }
        }
        return null;
    }

    private ControlMethodEntity findControlMethodByMarks(PlansEntity plan, Collection<String> controlTypes) {
        for (String controlType : controlTypes) {
            List<MarksEntity> marks = marksService.findMarksByPlanAndTypeControl(plan, controlType);
            if (marks != null && !marks.isEmpty()) {
                return marks.get(0).getControlMethod();
            }
        }
        return null;
    }

    private boolean isMatchingControl(ControlMethodEntity controlMethod, String controlType) {
        if (controlMethod == null || controlMethod.getName() == null || controlType == null) {
            return false;
        }

        String controlName = controlMethod.getName().trim();
        String normalizedControlName = controlName.toLowerCase(Locale.ROOT);
        String normalizedControlType = controlType.trim().toLowerCase(Locale.ROOT);

        return normalizedControlName.equals(normalizedControlType)
                || normalizedControlName.contains(normalizedControlType)
                || normalizedControlType.contains(normalizedControlName);
    }

    private String formatControlTypes(Collection<String> controlTypes) {
        if (controlTypes == null || controlTypes.isEmpty()) {
            return "";
        }
        return String.join(", ", controlTypes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList());
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
            ControlMethodEntity controlMethod = disciplineSummary.controlMethod();
            List<MarksEntity> marks = marksService.findMarksByPlanAndControlMethod(plan, controlMethod);
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

    private Map<Long, Integer> collectMarksByStudent(PlansEntity plan, ControlMethodEntity controlMethod) {
        if (plan == null || controlMethod == null) {
            return Map.of();
        }

        return marksService.findMarksByPlanAndControlMethod(plan, controlMethod).stream()
                .filter(mark -> mark.getStudent() != null && mark.getStudent().getId() != null)
                .collect(Collectors.toMap(
                        mark -> mark.getStudent().getId(),
                        mark -> Optional.ofNullable(mark.getFinalGrade()).orElse(0),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    private Map<String, List<SummaryReportPdfGenerator.ModuleMark>> buildTwoModuleMarksForSummaryReport(
            List<StudentEntity> students,
            List<TwoModuleDisciplineSummary> disciplineSummaries) {
        Map<String, List<SummaryReportPdfGenerator.ModuleMark>> marksByStudent = new LinkedHashMap<>();
        List<String> studentNames = students.stream()
                .map(StudentEntity::getFullName)
                .collect(Collectors.toList());

        for (String name : studentNames) {
            marksByStudent.put(name, new ArrayList<>());
        }

        for (TwoModuleDisciplineSummary disciplineSummary : disciplineSummaries) {
            Map<Long, Integer> firstModuleMarks = collectMarksByStudent(disciplineSummary.plan(), disciplineSummary.firstControlMethod());
            Map<Long, Integer> secondModuleMarks = collectMarksByStudent(disciplineSummary.plan(), disciplineSummary.secondControlMethod());

            for (int i = 0; i < students.size(); i++) {
                StudentEntity student = students.get(i);
                String studentName = studentNames.get(i);

                if (!disciplineSummary.assignedStudentIds().contains(student.getId())) {
                    marksByStudent.get(studentName).add(null);
                    continue;
                }

                Integer firstMark = disciplineSummary.firstControlMethod() == null
                        ? null
                        : firstModuleMarks.getOrDefault(student.getId(), 0);
                Integer secondMark = disciplineSummary.secondControlMethod() == null
                        ? null
                        : secondModuleMarks.getOrDefault(student.getId(), 0);

                marksByStudent.get(studentName).add(new SummaryReportPdfGenerator.ModuleMark(firstMark, secondMark));
            }
        }

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
            ControlMethodEntity controlMethod = assignment.controlMethod;
            if (controlMethod == null) {
                System.out.println("[SummaryReportService] Пропущено план без контрольної події");
                continue;
            }
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
            summaries.add(new DisciplineSummary(plan, controlMethod, title, elective, assignedStudentIds));
        }

        List<DisciplineSummary> sorted = summaries.stream()
                .sorted(Comparator.comparing(DisciplineSummary::title, ukrainianCollator))
                .collect(Collectors.toList());
        System.out.println("[SummaryReportService] Підсумок дисциплін сформовано: " + sorted.size());
        return sorted;
    }

    private List<TwoModuleDisciplineSummary> buildTwoModuleDisciplineSummaries(Collection<TwoModulePlanAssignment> assignments,
                                                                               List<StudentEntity> students) {
        if (assignments == null || assignments.isEmpty()) {
            return List.of();
        }

        Set<Long> groupStudentIds = students.stream()
                .map(StudentEntity::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<TwoModuleDisciplineSummary> summaries = new ArrayList<>();
        for (TwoModulePlanAssignment assignment : assignments) {
            PlansEntity plan = assignment.plan;
            DisciplineEntity discipline = plan.getDiscipline();
            if (discipline == null || discipline.getTitle() == null) {
                continue;
            }

            String title = discipline.getTitle().trim();
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

            summaries.add(new TwoModuleDisciplineSummary(
                    plan,
                    assignment.firstControlMethod,
                    assignment.secondControlMethod,
                    title,
                    elective,
                    assignedStudentIds
            ));
        }

        return summaries.stream()
                .sorted(Comparator.comparing(TwoModuleDisciplineSummary::title, ukrainianCollator))
                .collect(Collectors.toList());
    }

    private String safeDisciplineTitle(PlansEntity plan) {
        DisciplineEntity discipline = plan.getDiscipline();
        if (discipline == null || discipline.getTitle() == null) {
            return "<без назви>";
        }
        return discipline.getTitle();
    }

    private boolean isPlanFromSameProgram(PlansEntity plan, StudentGroupEntity group) {
        if (plan == null || group == null) {
            return false;
        }

        SpecialtyEntity planSpecialty = plan.getSpecialty();
        SpecialtyEntity groupSpecialty = group.getSpecialty();
        if (planSpecialty == null || groupSpecialty == null) {
            return false;
        }

        EduProgramEntity planProgram = planSpecialty.getEduProgram();
        EduProgramEntity groupProgram = groupSpecialty.getEduProgram();

        if (planProgram != null && groupProgram != null) {
            return Objects.equals(planProgram.getId(), groupProgram.getId());
        }

        return Objects.equals(planSpecialty.getId(), groupSpecialty.getId());
    }

    public record SummaryReportResult(String groupCode, byte[] pdfBytes) {
    }

    private record DisciplineSummary(PlansEntity plan,
                                     ControlMethodEntity controlMethod,
                                     String title,
                                     boolean elective,
                                     Set<Long> assignedStudentIds) {
    }

    private record TwoModuleDisciplineSummary(PlansEntity plan,
                                              ControlMethodEntity firstControlMethod,
                                              ControlMethodEntity secondControlMethod,
                                              String title,
                                              boolean elective,
                                              Set<Long> assignedStudentIds) {
    }

    private static class PlanAssignment {
        private final PlansEntity plan;
        private final ControlMethodEntity controlMethod;
        private final Set<Long> assignedStudentIds = new LinkedHashSet<>();

        private PlanAssignment(PlansEntity plan, ControlMethodEntity controlMethod) {
            this.plan = plan;
            this.controlMethod = controlMethod;
        }
    }

    private static class TwoModulePlanAssignment {
        private final PlansEntity plan;
        private final ControlMethodEntity firstControlMethod;
        private final ControlMethodEntity secondControlMethod;
        private final Set<Long> assignedStudentIds = new LinkedHashSet<>();

        private TwoModulePlanAssignment(PlansEntity plan,
                                        ControlMethodEntity firstControlMethod,
                                        ControlMethodEntity secondControlMethod) {
            this.plan = plan;
            this.firstControlMethod = firstControlMethod;
            this.secondControlMethod = secondControlMethod;
        }
    }

    public static class SummaryReportGenerationException extends RuntimeException {
        public SummaryReportGenerationException(String message) {
            super(message);
        }
    }
}
