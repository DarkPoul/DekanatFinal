package com.esvar.dekanat.service;

import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.dto.StudentOptionDTO;
import com.esvar.dekanat.dto.RatingFilterOptions;
import com.esvar.dekanat.entity.SpecialtyEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import com.esvar.dekanat.repository.GroupRepository;
import com.esvar.dekanat.security.SecurityService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupService {


    private final GroupRepository groupRepository;
    private final StudentService studentService;
    private final SecurityService securityService;
    private final FacultyService facultyService;

    public GroupService(GroupRepository groupRepository, StudentService studentService, SecurityService securityService, FacultyService facultyService) {
        this.groupRepository = groupRepository;
        this.studentService = studentService;
        this.securityService = securityService;
        this.facultyService = facultyService;
    }

    // Отримання всіх груп
    public List<StudentGroupEntity> getAllGroups() {
        return groupRepository.findAll();
    }

    public List<GroupDTO> getGroupsDTO() {
        // 1. Завантажуємо всі групи
        List<StudentGroupEntity> groups = groupRepository.findAll();

        // 2. Отримуємо ролі й roleType поточного користувача
        UserDetails user = securityService.getAuthenticatedUser();
        boolean isAdmin   = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isDekanat = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().startsWith("ROLE_DEKANAT"));
        String roleType   = securityService.getCurrentRoleType();

        // 3. Якщо користувач — методист, готуємо набір ID груп його факультету
        Set<Long> groupIdsForFaculty = isDekanat
                ? studentService.getGroupIdsByFaculty(Long.valueOf(roleType))
                : Collections.emptySet();

        Collator ukrainianCollator = Collator.getInstance(new Locale("uk", "UA"));

        // 4. Фільтруємо та мапимо
        return groups.stream()
                .filter(group -> !isDekanat || groupIdsForFaculty.contains(group.getId()))
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

    public List<Integer> getYears() {
        return getGroupsDTO().stream()
                .map(GroupDTO::getYear)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    public RatingFilterOptions getRatingFilterOptions() {
        List<GroupDTO> availableGroups = getGroupsDTO();
        List<String> specialties = availableGroups.stream()
                .map(GroupDTO::getSpecialtyAbbreviation)
                .distinct()
                .sorted()
                .toList();
        List<Integer> courses = availableGroups.stream()
                .map(GroupDTO::getCourse)
                .distinct()
                .sorted()
                .toList();
        List<Integer> groupNumbers = availableGroups.stream()
                .map(GroupDTO::getGroupNumber)
                .distinct()
                .sorted()
                .toList();
        List<Integer> years = availableGroups.stream()
                .map(GroupDTO::getYear)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();

        UserDetails user = securityService.getAuthenticatedUser();
        boolean isDekanat = user != null && user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().startsWith("ROLE_DEKANAT"));

        String defaultSpecialty = isDekanat && specialties.size() == 1 ? specialties.get(0) : null;
        Integer defaultCourse = isDekanat && courses.size() == 1 ? courses.get(0) : null;
        Integer defaultGroupNumber = isDekanat && groupNumbers.size() == 1 ? groupNumbers.get(0) : null;
        Integer defaultYear = years.isEmpty() ? null : years.get(0);

        return new RatingFilterOptions(
                availableGroups,
                specialties,
                courses,
                groupNumbers,
                years,
                defaultSpecialty,
                defaultCourse,
                defaultGroupNumber,
                defaultYear
        );
    }



    public List<StudentOptionDTO> getStudentOptionsForGroup(String groupSelectValue) {
        Collator ukrainianCollator = Collator.getInstance(new Locale("uk", "UA"));
        StudentGroupEntity group = groupRepository.findByGroupCode(groupSelectValue);
        if (group == null) {
            return Collections.emptyList();
        }

        return studentService.getStudentByGroupId(group.getId()).stream()
                .map(student -> new StudentOptionDTO(student.getId(), student.getFullName()))
                .sorted(Comparator.comparing(StudentOptionDTO::displayName, ukrainianCollator))
                .toList();
    }

    public List<String> getAllStudentsForSelectedGroup(String groupSelectValue) {
        Collator ukrainianCollator = Collator.getInstance(new Locale("uk", "UA"));
        return studentService.getStudentByGroupId(groupRepository.findByGroupCode(groupSelectValue).getId()).stream()
                .map(student -> student.getSurname() + " " + student.getName() + " " + student.getPatronymic())
                .sorted(ukrainianCollator)
                .collect(Collectors.toList());
    }

    public List<StudentEntity> getAllStudentsEntityForSelectedGroup(String groupSelectValue) {
        return studentService.getStudentByGroupId(groupRepository.findByGroupCode(groupSelectValue).getId());
    }

    /**
     * Отримує ID групи за її кодом.
     *
     * @param groupCode Код групи.
     * @return Long - ID групи або null, якщо група не знайдена.
     */
    public Long getGroupIdByCode(String groupCode) {
        if (groupCode == null || groupCode.isEmpty()) {
            return null;
        }

        return groupRepository.findIdByGroupCode(groupCode).orElse(null);
    }

    public StudentGroupEntity getGroupByTitle(String title){
        return groupRepository.findByGroupCode(title);
    }

    public StudentGroupEntity save(StudentGroupEntity group) {
        return groupRepository.save(group);
    }

    public void deleteById(Long id) {
        groupRepository.deleteById(id);
    }
}
