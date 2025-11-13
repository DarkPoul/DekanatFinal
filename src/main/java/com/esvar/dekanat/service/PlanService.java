package com.esvar.dekanat.service;

import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.dto.SpecialtyDTO;
import com.esvar.dekanat.entity.*;
import com.esvar.dekanat.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PlanService {
    private final PlanRepository planRepository;
    private final StudentPlansRepository studentPlansRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final SessionRepository sessionRepository;
    private final MarksService marksService;
    private final MarksPartsService marksPartsService;
    private final MarksInitializerService marksInitializerService;
    private final PlanStatementNumberService planStatementNumberService;


    public PlanService(PlanRepository planRepository, StudentPlansRepository studentPlansRepository, StudentRepository studentRepository, FacultyRepository facultyRepository, DepartmentRepository departmentRepository, SessionRepository sessionRepository, MarksService marksService, MarksPartsService marksPartsService, MarksInitializerService marksInitializerService, PlanStatementNumberService planStatementNumberService) {    this.planRepository = planRepository;
        this.studentPlansRepository = studentPlansRepository;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.sessionRepository = sessionRepository;
        this.marksService = marksService;
        this.marksPartsService = marksPartsService;
        this.marksInitializerService = marksInitializerService;
        this.planStatementNumberService = planStatementNumberService;
    }


    @Transactional
    public void savePlan(PlansEntity plan) {
        planStatementNumberService.assignNumber(plan);
        planRepository.save(plan);
        synchronizeGroupAssignments(plan);
        initializeMarksForAssignedStudents(plan);
        planStatementNumberService.createRecordsForPlan(plan);
    }

    public List<PlansEntity> getAllPlans() {
        return planRepository.findAll();
    }




    public List<PlansEntity> getAllPlansForGroupAndSemester(StudentGroupEntity group, int semester) {
        if (group == null) {
            return Collections.emptyList();
        }
        return planRepository.findByGroupAndSemester(group, semester);
    }


    /**
     * Отримує список імен студентів, які вибрали конкретний план.
     *
     * @param plan PlansEntity - план, для якого потрібно знайти студентів.
     * @return List<String> - список імен студентів.
     */
    @Transactional
    public List<String> getSelectedStudentsForPlan(PlansEntity plan) {
        if (plan == null) {
            return new ArrayList<>(); // Якщо план відсутній, повертаємо порожній список
        }

        // Отримуємо студентів за їх ID та формуємо список імен
        return studentPlansRepository.findByPlan(plan).stream()
                .map(sp -> sp.getStudent().getFullName())
                .collect(Collectors.toList());
    }

    /**
     * Оновлює існуючий навчальний план.
     *
     * @param updatedPlan PlansEntity - оновлений об'єкт плану.
     */
    @Transactional
    public void updatePlan(PlansEntity updatedPlan) {
        planStatementNumberService.updateForPlan(updatedPlan);
        updatePlan(updatedPlan, null);
    }

    @Transactional
    public void updatePlan(PlansEntity updatedPlan, List<StudentEntity> students) {
        if (updatedPlan == null || updatedPlan.getId() == null) {
            throw new IllegalArgumentException("ID плану повинен бути заданий.");
        }

        planRepository.save(updatedPlan);
        synchronizeGroupAssignments(updatedPlan);
        initializeMarksForAssignedStudents(updatedPlan);
        if (students != null && !students.isEmpty()) {
            marksInitializerService.initializeMarksForPlan(updatedPlan, students);
        }
    }

    // Метод для видалення плану за ID
    @Transactional
    public void deletePlanById(Long planId) {
        if (planId == null) {
            return;
        }
        marksPartsService.deleteByPlanId(planId);
        marksService.deleteByPlanId(planId);
        studentPlansRepository.deleteAllByPlanId(planId);
        planStatementNumberService.deleteByPlanId(planId);
        planRepository.deleteById(planId);
    }

    public void deletePlan(PlansEntity plan) {
        if (plan != null) {
            planStatementNumberService.deleteByPlanId(plan.getId());
            planRepository.delete(plan);
        }
    }

    public PlansEntity getPlanById(Long id) {
        return planRepository.findById(id).orElse(null);
    }

    public List<SpecialtyDTO> getSpecialtiesByFacultyAndDepartment(String faculty, String department) {
        Collator ukrainianCollator = Collator.getInstance(new Locale("uk", "UA"));

        return planRepository.findByFacultyAndDepartment(
                        facultyRepository.findByTitle(faculty),
                        departmentRepository.findByTitle(department)
                ).stream()
                .map(PlansEntity::getSpecialty)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        SpecialtyEntity::getAbbreviation,
                        specialty -> new SpecialtyDTO(
                                specialty.getAbbreviation(),
                                specialty.getTitle()
                        ),
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(SpecialtyDTO::getTitle, ukrainianCollator))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getCourseByFacultyAndDepartmentAndSpecialty(String faculty, String department, String specialty) {
        return planRepository.findByFacultyAndDepartmentAndSpecialty_Abbreviation(
                        facultyRepository.findByTitle(faculty),
                        departmentRepository.findByTitle(department),
                        specialty
                ).stream()
                .flatMap(plan -> plan.getGroups().stream())
                .map(StudentGroupEntity::getCourse)
                .map(String::valueOf)
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupDTO> getGroupsByFacultyAndDepartmentAndSpecialtyAndCourse(String faculty, String department, String specialty, int course) {
        Collator ukrainianCollator = Collator.getInstance(new Locale("uk", "UA"));

        List<StudentGroupEntity> uniqueGroups = planRepository.findByFacultyAndDepartmentAndSpecialty_AbbreviationAndGroup_Course(
                        facultyRepository.findByTitle(faculty),
                        departmentRepository.findByTitle(department),
                        specialty,
                        course
                ).stream()
                .flatMap(plan -> plan.getGroups().stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();

        return uniqueGroups.stream()
                .map(group -> new GroupDTO(
                        group.getGroupCode(),
                        group.getSpecialty().getAbbreviation(),
                        group.getCourse(),
                        group.getGroupNumber(),
                        group.getYear()
                ))
                .sorted(Comparator.comparing(GroupDTO::getGroupCode, ukrainianCollator))
                .collect(Collectors.toList());
    }

    public List<String> getDisciplinesByFacultyAndDepartmentAndSpecialtyAndGroupCourseAndGroupGroupNumber(String faculty, String department, String specialty, int course, int groupNumber) {
        int semester = getNumberSemester(String.valueOf(course));
        return planRepository.findByFacultyAndDepartmentAndSpecialty_AbbreviationAndGroup_CourseAndGroup_GroupNumber(
                        facultyRepository.findByTitle(faculty),
                        departmentRepository.findByTitle(department),
                        specialty,
                        course,
                        groupNumber
                ).stream()
                .filter(p -> p.getSemester() == semester)
                .map(PlansEntity::getDiscipline)
                .map(DisciplineEntity::getTitle)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> getControlTypesByFacultyAndDepartmentAndSpecialtyAndGroupCourseAndGroupNumberAndDiscipline(
            String faculty, String department, String specialty, int course, int groupNumber, String discipline) {
        int semester = getNumberSemester(String.valueOf(course));
        List<PlansEntity> plans = planRepository.findByFacultyAndDepartmentAndSpecialty_AbbreviationAndGroup_CourseAndGroup_GroupNumberAndDiscipline_Title(
                facultyRepository.findByTitle(faculty),
                departmentRepository.findByTitle(department),
                specialty,
                course,
                groupNumber,
                discipline
        );

        List<PlansEntity> semesterPlans = plans.stream()
                .filter(p -> p.getSemester() == semester)
                .toList();

        List<PlansEntity> relevantPlans = semesterPlans.isEmpty() ? plans : semesterPlans;

        List<String> controlTypes = relevantPlans.stream()
                .flatMap(plan -> Stream.of(plan.getFirstControl(), plan.getSecondControl()))
                .filter(Objects::nonNull)
                .map(ControlMethodEntity::getName)
                .filter(control -> !"Відсутній".equals(control))
                .distinct()
                .collect(Collectors.toList());

        if (shouldIncludeModuleControls(relevantPlans, course, groupNumber)) {
            controlTypes.add("Перший модульний контроль");
            controlTypes.add("Другий модульний контроль");
        }

        return controlTypes;
    }

    private boolean shouldIncludeModuleControls(List<PlansEntity> plans, int course, int groupNumber) {
        if (plans == null || plans.isEmpty()) {
            return false;
        }

        return findGroupForCourseAndNumber(plans, course, groupNumber)
                .map(this::isFullTimeGroup)
                .orElse(false);
    }

    private Optional<StudentGroupEntity> findGroupForCourseAndNumber(List<PlansEntity> plans, int course, int groupNumber) {
        return plans.stream()
                .flatMap(plan -> plan.getGroups().stream())
                .filter(Objects::nonNull)
                .filter(group -> group.getCourse() == course && group.getGroupNumber() == groupNumber)
                .findFirst();
    }

    private boolean isFullTimeGroup(StudentGroupEntity group) {
        if (group == null) {
            return false;
        }

        List<StudentEntity> students = studentRepository.findByGroup(group);
        if (students == null || students.isEmpty()) {
            return false;
        }

        return students.stream().allMatch(StudentEntity::isFullTime);
    }

    public PlansEntity getPlanEntityByFacultyAndDepartmentAndSpecialtyAndGroupCourseAndGroupNumberAndDiscipline(String faculty, String department, String specialty, int course, int groupNumber, String discipline){
        int semester = getNumberSemester(String.valueOf(course));
        return planRepository.findByFacultyAndDepartmentAndSpecialty_AbbreviationAndGroup_CourseAndGroup_GroupNumberAndDiscipline_Title(
                        facultyRepository.findByTitle(faculty),
                        departmentRepository.findByTitle(department),
                        specialty,
                        course,
                        groupNumber,
                        discipline
                ).stream()
                .filter(p -> p.getSemester() == semester)
                .findFirst().orElse(null);
    }

    private int getNumberSemester(String course) {
        boolean isWinter = sessionRepository.findById(1L).stream().map(SessionEntity::isWinter).findFirst().orElse(false);
        if (isWinter) {
            return (Integer.parseInt(course) * 2 - 1);
        } else {
            return Integer.parseInt(course) * 2;
        }
    }

    private void synchronizeGroupAssignments(PlansEntity plan) {
        if (plan == null || plan.getId() == null) {
            return;
        }
        if (plan.isElective()) {
            return;
        }
        Set<StudentGroupEntity> planGroups = plan.getGroups();
        if (planGroups == null || planGroups.isEmpty()) {
            return;
        }

        for (StudentGroupEntity group : planGroups) {
            if (group == null) {
                continue;
            }
            List<StudentEntity> students = studentRepository.findByGroup(group);
            for (StudentEntity student : students) {
                if (student == null) {
                    continue;
                }
                if (!studentPlansRepository.existsByStudentIdAndPlanId(student.getId(), plan.getId())) {
                    StudentPlansEntity mapping = new StudentPlansEntity();
                    mapping.setStudent(student);
                    mapping.setPlan(plan);
                    mapping.setId(new StudentPlansPK(student.getId(), plan.getId()));
                    studentPlansRepository.save(mapping);
                }
            }
        }
    }

    private void initializeMarksForAssignedStudents(PlansEntity plan) {
        if (plan == null || plan.getId() == null) {
            return;
        }
        List<StudentEntity> assignedStudents = studentPlansRepository.findByPlan(plan).stream()
                .map(StudentPlansEntity::getStudent)
                .filter(Objects::nonNull)
                .toList();
        if (!assignedStudents.isEmpty()) {
            marksInitializerService.initializeMarksForPlan(plan, assignedStudents);
        }
    }
}
