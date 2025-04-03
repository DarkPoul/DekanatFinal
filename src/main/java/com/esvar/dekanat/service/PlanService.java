package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.*;
import com.esvar.dekanat.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PlanService {
    private final PlanRepository planRepository;
    private final StudentPlansRepository studentPlansRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final SpecialtyRepository specialtyRepository;
    private final DisciplineRepository disciplineRepository;
    private final SessionRepository sessionRepository;

    public PlanService(PlanRepository planRepository, StudentPlansRepository studentPlansRepository, StudentRepository studentRepository, FacultyRepository facultyRepository, DepartmentRepository departmentRepository, SpecialtyRepository specialtyRepository, DisciplineRepository disciplineRepository, SessionRepository sessionRepository) {
        this.planRepository = planRepository;
        this.studentPlansRepository = studentPlansRepository;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.specialtyRepository = specialtyRepository;
        this.disciplineRepository = disciplineRepository;
        this.sessionRepository = sessionRepository;
    }


    @Transactional
    public void savePlan(PlansEntity plan) {
        planRepository.save(plan);
    }

    public List<PlansEntity> getAllPlans() {
        return planRepository.findAll();
    }




    public List<PlansEntity> getAllPlansForGroupAndSemester(StudentGroupEntity group, int semester) {

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
        if (updatedPlan == null || updatedPlan.getId() == null) {
            throw new IllegalArgumentException("ID плану повинен бути заданий.");
        }

        // Оновлюємо запис у БД
        planRepository.save(updatedPlan);
    }

    // Метод для видалення плану за ID
    @Transactional
    public void deletePlanById(Long planId) {
        studentPlansRepository.deleteAllByPlanId(planId);
        planRepository.deleteById(planId);
    }

    public void deletePlan(PlansEntity plan) {
        planRepository.delete(plan);
    }

    public PlansEntity getPlanById(Long id) {
        return planRepository.findById(id).orElse(null);
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

    public List<String> getCourseByFacultyAndDepartmentAndSpecialty(String faculty, String department, String specialty) {
        return planRepository.findByFacultyAndDepartmentAndSpecialty
                (
                        facultyRepository.findByTitle(faculty),
                        departmentRepository.findByTitle(department),
                        specialtyRepository.findByAbbreviation(specialty)
                ).stream()
                .map(PlansEntity::getGroup)
                .map(StudentGroupEntity::getCourse)
                .map(String::valueOf)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> getNumGroupsByFacultyAndDepartmentAndSpecialtyAndCourse(String faculty, String department, String specialty, int course) {
        return planRepository.findByFacultyAndDepartmentAndSpecialtyAndGroup_Course
                (
                        facultyRepository.findByTitle(faculty),
                        departmentRepository.findByTitle(department),
                        specialtyRepository.findByAbbreviation(specialty),
                        course
                ).stream()
                .map(PlansEntity::getGroup)
                .map(StudentGroupEntity::getGroupNumber)
                .map(String::valueOf)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> getDisciplinesByFacultyAndDepartmentAndSpecialtyAndGroupCourseAndGroupGroupNumber(String faculty, String department, String specialty, int course, int groupNumber) {
        return planRepository.findByFacultyAndDepartmentAndSpecialtyAndGroup_CourseAndGroup_GroupNumber
                (
                        facultyRepository.findByTitle(faculty),
                        departmentRepository.findByTitle(department),
                        specialtyRepository.findByAbbreviation(specialty),
                        course,
                        groupNumber
                ).stream()
                .map(PlansEntity::getDiscipline)
                .map(DisciplineEntity::getTitle)
                .collect(Collectors.toList());
    }

    public List<String> getControlTypesByFacultyAndDepartmentAndSpecialtyAndGroupCourseAndGroupNumberAndDiscipline(
            String faculty, String department, String specialty, int course, int groupNumber, String discipline) {

        List<String> controlTypes = planRepository.findByFacultyAndDepartmentAndSpecialtyAndGroup_CourseAndGroup_GroupNumberAndDiscipline(
                        facultyRepository.findByTitle(faculty),
                        departmentRepository.findByTitle(department),
                        specialtyRepository.findByAbbreviation(specialty),
                        course,
                        groupNumber,
                        disciplineRepository.findByTitle(discipline)
                ).stream()
                .flatMap(plan -> Stream.of(plan.getFirstControl().getName(), plan.getSecondControl().getName())) // Отримуємо обидва значення
                .filter(control -> !"Відсутній".equals(control)) // Фільтруємо "Відсутній"
                .distinct() // Унікальні значення (якщо потрібно)
                .collect(Collectors.toList());

        // Додаємо "Перший модульний контроль" і "Другий модульний контроль"
        controlTypes.add("Перший модульний контроль");
        controlTypes.add("Другий модульний контроль");

        return controlTypes;
    }

    public PlansEntity getPlanEntityByFacultyAndDepartmentAndSpecialtyAndGroupCourseAndGroupNumberAndDiscipline(String faculty, String department, String specialty, int course, int groupNumber, String discipline){
        return planRepository.findByFacultyAndDepartmentAndSpecialtyAndGroup_CourseAndGroup_GroupNumberAndDiscipline(
                facultyRepository.findByTitle(faculty),
                departmentRepository.findByTitle(department),
                specialtyRepository.findByAbbreviation(specialty),
                course,
                groupNumber,
                disciplineRepository.findByTitle(discipline)
        ).stream().findFirst().orElse(null);
    }

    private int getNumberSemester(String course) {
        boolean isWinter = sessionRepository.findById(1L).stream().map(SessionEntity::isWinter).findFirst().orElse(false);
        if (isWinter) {
            return (Integer.parseInt(course) * 2 - 1);
        } else {
            return Integer.parseInt(course) * 2;
        }
    }
}
