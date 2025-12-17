package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentPlansEntity;
import com.esvar.dekanat.entity.StudentPlansPK;
import com.esvar.dekanat.repository.StudentPlansRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentPlansService{

    private final StudentPlansRepository studentPlansRepository;
    private final StudentService studentService;


    public StudentPlansService(StudentPlansRepository studentPlansRepository, StudentService studentService) {
        this.studentPlansRepository = studentPlansRepository;
        this.studentService = studentService;
    }

    /**
     * Зберігає пов'язання між студентом і навчальним планом.
     *
     * @param studentPlan StudentPlansEntity - об'єкт для збереження.
     */
    public void saveStudentPlan(StudentPlansEntity studentPlan) {
        if (studentPlan == null || studentPlan.getStudent() == null || studentPlan.getPlan() == null) {
            throw new IllegalArgumentException("Студент і план повинні бути задані.");
        }

        // Перевіряємо, чи запис вже існує (унікальність за (student_id, plan_id))
        boolean exists = studentPlansRepository.existsByStudentIdAndPlanId(
                studentPlan.getStudent().getId(),
                studentPlan.getPlan().getId()
        );

        if (!exists) {
            // Якщо запис не існує, зберігаємо новий
            studentPlan.setId(new StudentPlansPK(
                    studentPlan.getStudent().getId(),
                    studentPlan.getPlan().getId()
            ));
            studentPlansRepository.save(studentPlan);
        } else {
            // Якщо запис вже існує, можна або проігнорувати, або оновити його

        }
    }

    /**
     * Оновлює записи у таблиці student_plans для певного плану.
     *
     * @param updatedPlan PlansEntity - оновлений план.
     * @param students    List<String> - список імен студентів.
     */
    @Transactional
    public List<StudentEntity> updateStudentPlans(PlansEntity updatedPlan, List<String> students) {
        if (updatedPlan == null || updatedPlan.getId() == null) {
            throw new IllegalArgumentException("План для оновлення повинен бути заданий.");
        }

        List<StudentEntity> mappedStudents = new ArrayList<>();
        if (students != null) {
            for (String studentName : students) {
                mappedStudents.add(studentService.getStudentByFullName(studentName));
            }
        }

        return synchronizePlanAssignments(updatedPlan, mappedStudents);
    }


    public void deleteStudentPlansByPlan(PlansEntity plan) {
        studentPlansRepository.deleteByPlan(plan);
    }

    @Transactional
    public void deleteByPlanId(Long planId) {
        studentPlansRepository.deleteByPlanId(planId); // Викликаємо кастомний запит
    }

    public List<StudentEntity> getStudentByPlan(PlansEntity plan) {
        return studentPlansRepository.findByPlan(plan)
                .stream()
                .map(StudentPlansEntity::getStudent)
                .collect(Collectors.toList());
    }

    /**
     * Повертає усі записи student_plans для вказаного студента.
     *
     * @param student студент
     * @return список StudentPlansEntity
     */
    public List<StudentPlansEntity> getPlansForStudent(StudentEntity student) {
        if (student == null) {
            return new ArrayList<>();
        }
        return studentPlansRepository.findByStudent(student);
    }

    @Transactional
    public List<StudentEntity> synchronizePlanAssignments(PlansEntity plan, List<StudentEntity> students) {
        if (plan == null || plan.getId() == null) {
            throw new IllegalArgumentException("План повинен бути заданий.");
        }

        List<StudentEntity> targetStudents = students == null ? List.of() : students;
        return synchronizeInternal(plan, targetStudents);
    }

    private List<StudentEntity> synchronizeInternal(PlansEntity plan, List<StudentEntity> targetStudents) {
        List<StudentPlansEntity> existingLinks = studentPlansRepository.findByPlan(plan);
        List<StudentEntity> existingStudents = existingLinks.stream()
                .map(StudentPlansEntity::getStudent)
                .toList();

        for (StudentEntity student : targetStudents) {
            boolean exists = existingStudents.stream()
                    .anyMatch(s -> s.getId().equals(student.getId()));
            if (!exists) {
                StudentPlansEntity studentPlan = new StudentPlansEntity();
                studentPlan.setStudent(student);
                studentPlan.setPlan(plan);
                studentPlan.setId(new StudentPlansPK(student.getId(), plan.getId()));
                studentPlansRepository.save(studentPlan);
            }
        }

        List<Long> desiredIds = targetStudents.stream()
                .map(StudentEntity::getId)
                .toList();

        List<StudentEntity> toRemove = existingStudents.stream()
                .filter(s -> !desiredIds.contains(s.getId()))
                .toList();

        if (!toRemove.isEmpty()) {
            List<Long> removeIds = toRemove.stream().map(StudentEntity::getId).toList();
            studentPlansRepository.deleteByPlanIdAndStudentIds(plan.getId(), removeIds);
        }

        return targetStudents;
    }

}
