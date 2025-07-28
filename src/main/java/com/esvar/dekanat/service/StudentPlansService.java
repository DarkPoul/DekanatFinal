package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentPlansEntity;
import com.esvar.dekanat.entity.StudentPlansPK;
import com.esvar.dekanat.repository.StudentPlansRepository;
import com.esvar.dekanat.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StudentPlansService{

    private final StudentPlansRepository studentPlansRepository;
    private final StudentRepository studentRepository;


    public StudentPlansService(StudentPlansRepository studentPlansRepository, StudentRepository studentRepository) {
        this.studentPlansRepository = studentPlansRepository;
        this.studentRepository = studentRepository;
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
            System.out.println("Пов'язання між студентом і планом вже існує.");
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

        List<StudentPlansEntity> existingLinks = studentPlansRepository.findByPlan(updatedPlan);
        List<StudentEntity> existingStudents = existingLinks.stream()
                .map(StudentPlansEntity::getStudent)
                .toList();

        List<StudentEntity> newStudents = new ArrayList<>();
        if (students != null) {
            for (String studentName : students) {
                StudentEntity student = Optional.ofNullable(studentRepository.findBySurnameAndNameAndPatronymic(
                        studentName.split(" ")[0],
                        studentName.split(" ")[1],
                        studentName.split(" ")[2]
                )).orElseThrow(() -> new IllegalArgumentException("Студент '" + studentName + "' не знайдений."));
                newStudents.add(student);
                boolean exists = existingStudents.stream()
                        .anyMatch(s -> s.getId().equals(student.getId()));
                if (!exists) {
                    StudentPlansEntity studentPlan = new StudentPlansEntity();
                    studentPlan.setStudent(student);
                    studentPlan.setPlan(updatedPlan);
                    studentPlan.setId(new StudentPlansPK(student.getId(), updatedPlan.getId()));
                    studentPlansRepository.save(studentPlan);
                }
            }
        }

        List<Long> newIds = newStudents.stream().map(StudentEntity::getId).toList();
        List<StudentEntity> toRemove = existingStudents.stream()
                .filter(s -> !newIds.contains(s.getId()))
                .toList();
        if (!toRemove.isEmpty()) {
            List<Long> removeIds = toRemove.stream().map(StudentEntity::getId).toList();
            studentPlansRepository.deleteByPlanIdAndStudentIds(updatedPlan.getId(), removeIds);
        }

        return newStudents;
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

}
