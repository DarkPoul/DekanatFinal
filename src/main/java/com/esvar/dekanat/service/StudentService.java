package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import com.esvar.dekanat.repository.GroupRepository;
import com.esvar.dekanat.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StudentService {
    private final StudentRepository studentRepository;
    private final GroupRepository groupRepository;

    public StudentService(StudentRepository studentRepository, GroupRepository groupRepository) {
        this.studentRepository = studentRepository;
        this.groupRepository = groupRepository;
    }

    public List<StudentEntity> getStudentByGroupId(long groupId) {
        return studentRepository.findByGroupId(groupId);
    }

    public StudentEntity getStudentByFullName(String studentSurname, String studentName, String studentPatronymic) {
        return studentRepository.findBySurnameAndNameAndPatronymic(studentSurname, studentName, studentPatronymic);
    }


    public StudentEntity getStudentForCard(String selectGroupValue, String selectStudentValue) {
        Long groupId = groupRepository.findIdByGroupCode(selectGroupValue).orElseThrow();
        System.out.println("groupId "+groupId);
        System.out.println("groupEntities "+studentRepository.findByGroupId(groupId).stream().map(StudentEntity::getFullName));

        List<StudentEntity> studentEntities = studentRepository.findByGroupId(groupId);
        for (StudentEntity studentEntity : studentEntities) {
            System.out.println("studentEntity "+studentEntity.getFullName());
        }

        return studentRepository.findByGroupId(groupId)
                .stream()
                .filter(student -> student.getFullName().equals(selectStudentValue))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Не знайдено студента " + selectStudentValue + " у групі " + selectGroupValue));
    }

    public List<StudentEntity> getStudentsForCard(String selectGroupValue) {
        return studentRepository.findByGroup(groupRepository.findByGroupCode(selectGroupValue));
    }

    public void save(StudentEntity studentEntity) {
        studentRepository.save(studentEntity);
    }

    public StudentEntity getStudentByStudentPIB_AndGroup(String studentPIB, StudentGroupEntity group) {
        if (studentPIB == null) {
            throw new IllegalArgumentException("ПІБ студента не може бути порожнім.");
        }
        if (group == null) {
            throw new IllegalArgumentException("Група студента не може бути порожньою.");
        }

        String normalizedFullName = normalizeFullName(studentPIB);
        if (normalizedFullName.isBlank()) {
            throw new IllegalArgumentException("ПІБ студента не може бути порожнім.");
        }

        String[] parts = normalizedFullName.split(" ");
        if (parts.length >= 3) {
            String surname = parts[0];
            String name = parts[1];
            String patronymic = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
            StudentEntity student = studentRepository.findBySurnameAndNameAndPatronymicAndGroup_GroupCode(
                    surname,
                    name,
                    patronymic,
                    group.getGroupCode()
            );
            if (student != null) {
                return student;
            }
        }

        String normalizedTarget = normalizedFullName.toLowerCase();
        return studentRepository.findByGroup(group).stream()
                .filter(existing -> normalizeFullName(existing.getFullName()).equalsIgnoreCase(normalizedTarget))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Студента '" + normalizedFullName + "' у групі '" + group.getGroupCode() + "' не знайдено."
                ));
    }


    public StudentEntity findStudentById(Long id) {
        return studentRepository.findById(id).orElseThrow();
    }

    public List<StudentEntity> getAllStudents() {
        return studentRepository.findAll();
    }

    public StudentEntity getStudentByFullName(String fullName) {
        if (fullName == null) {
            throw new IllegalArgumentException("ПІБ студента не може бути порожнім.");
        }

        String normalizedFullName = normalizeFullName(fullName);
        if (normalizedFullName.isBlank()) {
            throw new IllegalArgumentException("ПІБ студента не може бути порожнім.");
        }

        String[] parts = normalizedFullName.split(" ");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Невірний формат ПІБ студента: '" + normalizedFullName + "'.");
        }

        String surname = parts[0];
        String name = parts[1];
        String patronymic = parts[2];

        StudentEntity student = Optional.ofNullable(getStudentByFullName(surname, name, patronymic))
                .orElseGet(() -> studentRepository.findAll()
                        .stream()
                        .filter(existing -> normalizeFullName(existing.getFullName()).equalsIgnoreCase(normalizedFullName))
                        .findFirst()
                        .orElse(null));

        if (student == null) {
            throw new IllegalArgumentException("Студента '" + normalizedFullName + "' не знайдено.");
        }

        return student;
    }

    private static String normalizeFullName(String fullName) {
        return Arrays.stream(fullName.trim().split("\\s+"))
                .map(StudentService::sanitizeNamePart)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(" "));
    }

    private static String sanitizeNamePart(String part) {
        if (part == null) {
            return "";
        }
        return part.replaceAll("^[^\\p{L}0-9]+|[^\\p{L}0-9]+$", "");
    }
}
