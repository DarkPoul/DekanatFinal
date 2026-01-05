package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import com.esvar.dekanat.repository.GroupRepository;
import com.esvar.dekanat.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.util.*;
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
        Collator ukrainianCollator = Collator.getInstance(new Locale("uk", "UA"));
        return studentRepository.findByGroupId(groupId).stream()
                .sorted(Comparator.comparing(StudentEntity::getFullName, ukrainianCollator))
                .collect(Collectors.toList());
    }

    public List<StudentEntity> getStudentsByIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return studentRepository.findByIdIn(studentIds);
    }

    public StudentEntity getStudentByFullName(String studentSurname, String studentName, String studentPatronymic) {
        NameParts parts = buildNameParts(studentSurname, studentName, studentPatronymic);
        if (!hasText(parts.surname()) || !hasText(parts.name())) {
            return null;
        }

        List<StudentEntity> candidates = studentRepository
                .findBySurnameIgnoreCaseAndNameIgnoreCaseOrderByIdAsc(parts.surname(), parts.name());

        return selectBestMatch(candidates, parts).orElse(null);
    }


    public StudentEntity getStudentForCard(String selectGroupValue, String selectStudentValue) {
        Long groupId = groupRepository.findIdByGroupCode(selectGroupValue).orElseThrow();
        List<StudentEntity> studentEntities = studentRepository.findByGroupId(groupId);


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
        NameParts parts = parseFullName(studentPIB);
        if (group == null) {
            throw new IllegalArgumentException("Група студента не може бути порожньою.");
        }

        List<StudentEntity> candidates = studentRepository
                .findBySurnameIgnoreCaseAndNameIgnoreCaseAndGroup_GroupCodeOrderByIdAsc(parts.surname(), parts.name(), group.getGroupCode());

        if (candidates.isEmpty()) {
            candidates = studentRepository.findByGroup(group).stream()
                    .sorted(Comparator.comparing(StudentEntity::getId))
                    .filter(existing -> normalizeFullNameFromEntity(existing).equalsIgnoreCase(parts.normalizedFullName()))
                    .toList();
        }

        return selectBestMatch(candidates, parts)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Студента '" + parts.normalizedFullName() + "' у групі '" + group.getGroupCode() + "' не знайдено."
                ));
    }

    public Set<Long> getGroupIdsByFaculty(Long facultyId) {
        if (facultyId == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(studentRepository.findDistinctGroupIdsByFacultyId(facultyId));
    }

    public StudentEntity findStudentById(Long id) {
        return studentRepository.findById(id).orElseThrow();
    }

    public List<StudentEntity> getAllStudents() {
        return studentRepository.findAll();
    }

    public StudentEntity getStudentByFullName(String fullName) {
        NameParts parts = parseFullName(fullName);
        StudentEntity student = findStudentByParts(parts);

        if (student == null) {
            throw new IllegalArgumentException("Студента '" + parts.normalizedFullName() + "' не знайдено.");
        }

        return student;
    }

    private StudentEntity findStudentByParts(NameParts parts) {
        List<StudentEntity> candidates = collectCandidates(parts);
        return selectBestMatch(candidates, parts).orElse(null);
    }

    private List<StudentEntity> collectCandidates(NameParts parts) {
        List<StudentEntity> candidates = studentRepository
                .findBySurnameIgnoreCaseAndNameIgnoreCaseOrderByIdAsc(parts.surname(), parts.name());

        if (!candidates.isEmpty()) {
            return candidates;
        }

        String normalizedTarget = parts.normalizedFullName();
        return studentRepository.findAll().stream()
                .sorted(Comparator.comparing(StudentEntity::getId))
                .filter(existing -> normalizeFullNameFromEntity(existing).equalsIgnoreCase(normalizedTarget))
                .toList();
    }

    private static Optional<StudentEntity> selectBestMatch(List<StudentEntity> candidates, NameParts target) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        String targetPatronymic = normalizeNamePart(target.patronymic());
        boolean targetHasPatronymic = hasText(targetPatronymic);
        Comparator<StudentEntity> byId = Comparator.comparing(StudentEntity::getId);

        if (targetHasPatronymic) {
            Optional<StudentEntity> patronymicMatch = candidates.stream()
                    .filter(student -> hasText(student.getPatronymic()))
                    .filter(student -> normalizeNamePart(student.getPatronymic()).equalsIgnoreCase(targetPatronymic))
                    .min(byId);

            if (patronymicMatch.isPresent()) {
                return patronymicMatch;
            }
        }

        Optional<StudentEntity> withoutPatronymic = candidates.stream()
                .filter(student -> !hasText(student.getPatronymic()))
                .min(byId);

        if (withoutPatronymic.isPresent()) {
            return withoutPatronymic;
        }

        if (!targetHasPatronymic) {
            return candidates.stream().min(byId);
        }

        return candidates.stream()
                .filter(student -> hasText(student.getPatronymic()))
                .min(byId);
    }

    private static NameParts buildNameParts(String surname, String name, String patronymic) {
        String normalizedSurname = normalizeNamePart(surname);
        String normalizedName = normalizeNamePart(name);
        String normalizedPatronymic = normalizeNamePart(patronymic);
        String normalizedFullName = normalizeFullName(String.join(" ",
                Arrays.asList(normalizedSurname, normalizedName, normalizedPatronymic)));
        return new NameParts(normalizedSurname, normalizedName, normalizedPatronymic, normalizedFullName);
    }

    private static NameParts parseFullName(String fullName) {
        if (fullName == null) {
            throw new IllegalArgumentException("ПІБ студента не може бути порожнім.");
        }

        String normalizedFullName = normalizeFullName(fullName);
        if (normalizedFullName.isBlank()) {
            throw new IllegalArgumentException("ПІБ студента не може бути порожнім.");
        }

        String[] parts = normalizedFullName.split(" ");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Невірний формат ПІБ студента: '" + normalizedFullName + "'.");
        }

        String surname = parts[0];
        String name = parts[1];
        String patronymic = parts.length > 2
                ? String.join(" ", Arrays.copyOfRange(parts, 2, parts.length))
                : "";

        return new NameParts(surname, name, patronymic, normalizedFullName);
    }

    private static String normalizeFullName(String fullName) {
        if (fullName == null) {
            return "";
        }
        return Arrays.stream(fullName.trim().split("\\s+"))
                .map(StudentService::normalizeNamePart)
                .filter(StudentService::hasText)
                .filter(part -> !part.equalsIgnoreCase("null"))
                .collect(Collectors.joining(" "));
    }

    private static String normalizeNamePart(String part) {
        if (part == null) {
            return "";
        }
        return sanitizeNamePart(part).trim();
    }

    private static String sanitizeNamePart(String part) {
        if (part == null) {
            return "";
        }
        return part.replaceAll("^[^\\p{L}0-9]+|[^\\p{L}0-9]+$", "");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeFullNameFromEntity(StudentEntity student) {
        return normalizeFullName(String.join(" ", Arrays.asList(
                normalizeNamePart(student.getSurname()),
                normalizeNamePart(student.getName()),
                normalizeNamePart(student.getPatronymic())
        )));
    }

    private record NameParts(String surname, String name, String patronymic, String normalizedFullName) {
    }
}
