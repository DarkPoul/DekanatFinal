package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentPlansEntity;
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
    public void updateStudentPlans(PlansEntity updatedPlan, List<String> students) {
        if (updatedPlan == null || updatedPlan.getId() == null) {
            throw new IllegalArgumentException("План для оновлення повинен бути заданий.");
        }

        if (students == null || students.isEmpty()) {
            return; // Якщо немає студентів, просто завершуємо роботу
        }

        // Видаляємо всі старі записи для даного плану
        studentPlansRepository.deleteAllByPlanId(updatedPlan.getId());

        // Створюємо нові записи для кожного студента
        for (String studentName : students) {
            StudentEntity student = Optional.ofNullable(studentRepository.findBySurnameAndNameAndPatronymic(
                    studentName.split(" ")[0],
                    studentName.split(" ")[1],
                    studentName.split(" ")[2]
            )).orElseThrow(() -> new IllegalArgumentException("Студент '" + studentName + "' не знайдений."));

            StudentPlansEntity studentPlan = new StudentPlansEntity();
            studentPlan.setStudent(student);
            studentPlan.setPlan(updatedPlan);

            // Зберігаємо новий запис
            studentPlansRepository.save(studentPlan);
        }
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

}
