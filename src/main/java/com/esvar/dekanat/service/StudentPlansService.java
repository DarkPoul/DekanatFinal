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
import java.util.Arrays;
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
                mappedStudents.add(findStudentByFullName(studentName));
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

    private StudentEntity findStudentByFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("ПІБ студента не може бути порожнім.");
        }

        String normalizedFullName = normalizeFullName(fullName);
        if (normalizedFullName.isBlank()) {
            throw new IllegalArgumentException("ПІБ студента не може бути порожнім.");
        }

        String[] parts = normalizedFullName.split(" ");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Невірний формат ПІБ студента: '" + fullName + "'.");
        }

        String surname = parts[0];
        String name = parts[1];
        String patronymic = parts.length > 2
                ? String.join(" ", Arrays.copyOfRange(parts, 2, parts.length))
                : null;

        StudentEntity directMatch = studentRepository.findFirstBySurnameAndNameAndPatronymicOrderByIdAsc(surname, name, patronymic);
        if (directMatch != null) {
            return directMatch;
        }

        String normalizedWithoutPatronymic = patronymic == null ? surname + " " + name : null;

        return studentRepository.findAll().stream()
                .filter(existing -> matchesFullName(existing, normalizedFullName, normalizedWithoutPatronymic))
                .sorted((a, b) -> compareByPatronymicPresence(a, b, normalizedWithoutPatronymic != null))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Студент '" + fullName + "' не знайдений."));
    }

    private static boolean matchesFullName(StudentEntity student,
                                           String normalizedFullName,
                                           String normalizedWithoutPatronymic) {
        String existingNormalized = normalizeFullName(student.getFullName());
        if (existingNormalized.equalsIgnoreCase(normalizedFullName)) {
            return true;
        }

        if (normalizedWithoutPatronymic == null || existingNormalized.isBlank()) {
            return false;
        }

        if (existingNormalized.equalsIgnoreCase(normalizedWithoutPatronymic)) {
            return true;
        }

        String[] parts = existingNormalized.split(" ");
        if (parts.length < 2) {
            return false;
        }

        String candidateWithoutPatronymic = parts[0] + " " + parts[1];
        return candidateWithoutPatronymic.equalsIgnoreCase(normalizedWithoutPatronymic);
    }

    private static int compareByPatronymicPresence(StudentEntity first,
                                                   StudentEntity second,
                                                   boolean preferMissingPatronymic) {
        if (!preferMissingPatronymic) {
            return 0;
        }

        boolean firstHasPatronymic = hasText(first.getPatronymic());
        boolean secondHasPatronymic = hasText(second.getPatronymic());

        if (firstHasPatronymic == secondHasPatronymic) {
            return 0;
        }
        return firstHasPatronymic ? 1 : -1;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeFullName(String fullName) {
        if (fullName == null) {
            return "";
        }
        return Arrays.stream(fullName.trim().split("\\s+"))
                .map(StudentPlansService::sanitizeNamePart)
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .filter(part -> !part.equalsIgnoreCase("null"))
                .collect(Collectors.joining(" "));
    }

    private static String sanitizeNamePart(String part) {
        if (part == null) {
            return "";
        }
        return part.replaceAll("^[^\\p{L}0-9]+|[^\\p{L}0-9]+$", "");
    }

}