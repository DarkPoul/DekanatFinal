package com.esvar.dekanat.service;

import com.esvar.dekanat.dto.GroupDTO;
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

    @Transactional(readOnly = true)
    public PlansEntity getPlanWithGroups(Long id) {
        if (id == null) {
            return null;
        }
        PlansEntity plan = planRepository.findById(id).orElse(null);
        if (plan != null && plan.getGroups() != null) {
            plan.getGroups().size();
        }
        return plan;
    }

    public List<String> getSpecialtiesByFacultyAndDepartment(String faculty, String department) {
        return planRepository.findByFacultyAndDepartment
                (
                        facultyRepository.findByTitle(faculty),
                        departmentRepository.findByTitle(department)
                ).stream()
                .map(PlansEntity::getSpecialty)
                .map(SpecialtyEntity::getAbbreviation)
                .distinct()
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

    public List<String> getDisciplinesByGroup(StudentGroupEntity group) {
        return getDisciplinesByGroup(group, null);
    }

    public List<String> getDisciplinesByGroup(StudentGroupEntity group, Long departmentId) {
        if (group == null) {
            return Collections.emptyList();
        }
        int semester = getNumberSemester(String.valueOf(group.getCourse()));
        List<PlansEntity> plans = departmentId == null
                ? planRepository.findByGroupAndSemester(group, semester)
                : planRepository.findByGroupAndSemesterAndDepartment(group, semester, departmentId);

        return plans.stream()
                .map(PlansEntity::getDiscipline)
                .map(DisciplineEntity::getTitle)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> getControlTypesByGroupAndDiscipline(StudentGroupEntity group, String discipline) {
        return getControlTypesByGroupAndDiscipline(group, discipline, null);
    }

    public List<String> getControlTypesByGroupAndDiscipline(StudentGroupEntity group, String discipline, Long departmentId) {
        if (group == null || discipline == null) {
            return Collections.emptyList();
        }
        int semester = getNumberSemester(String.valueOf(group.getCourse()));
        List<PlansEntity> plans = departmentId == null
                ? planRepository.findByGroupAndSemesterAndDiscipline_Title(group, semester, discipline)
                : planRepository.findByGroupAndSemesterAndDiscipline_TitleAndDepartment(group, semester, discipline, departmentId);

        List<String> controlTypes = plans.stream()
                .flatMap(plan -> Stream.of(plan.getFirstControl().getName(), plan.getSecondControl().getName()))
                .filter(control -> !"Відсутній".equals(control))
                .distinct()
                .collect(Collectors.toList());

        controlTypes.add("Перший модульний контроль");
        controlTypes.add("Другий модульний контроль");

        return controlTypes;
    }

    public PlansEntity getPlanEntityByGroupAndDiscipline(StudentGroupEntity group, String discipline){
        return getPlanEntityByGroupAndDiscipline(group, discipline, null);
    }

    public PlansEntity getPlanEntityByGroupAndDiscipline(StudentGroupEntity group, String discipline, Long departmentId){
        if (group == null || discipline == null) {
            return null;
        }
        int semester = getNumberSemester(String.valueOf(group.getCourse()));
        List<PlansEntity> plans = departmentId == null
                ? planRepository.findByGroupAndSemesterAndDiscipline_Title(group, semester, discipline)
                : planRepository.findByGroupAndSemesterAndDiscipline_TitleAndDepartment(group, semester, discipline, departmentId);

        return plans.stream()
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
        PlansEntity managedPlan = planRepository.findById(plan.getId()).orElse(null);
        if (managedPlan == null) {
            return;
        }

        Set<StudentGroupEntity> planGroups = managedPlan.getGroups();
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
