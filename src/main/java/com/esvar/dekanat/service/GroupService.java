package com.esvar.dekanat.service;

import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import com.esvar.dekanat.repository.GroupRepository;
import com.esvar.dekanat.security.SecurityService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.esvar.dekanat.user.UserModel;

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
        List<StudentGroupEntity> groups = groupRepository.findAll();

        UserDetails user = securityService.getAuthenticatedUser();
        boolean isDekanat = user != null && user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().startsWith("ROLE_DEKANAT"));

        RoleScope roleScope = parseRoleScope(securityService.getCurrentRoleType());
        Boolean preferredFullTime = roleScope.fullTime();

        if (preferredFullTime == null) {
            preferredFullTime = securityService.getCurrentUserModel()
                    .map(UserModel::getRole)
                    .filter(role -> role != null && role.startsWith("ROLE_DEKANAT"))
                    .map(this::parseStudyFormToken)
                    .orElse(null);
        }

        Set<Long> groupIdsForFaculty = null;
        if (isDekanat && roleScope.facultyId() != null) {
            groupIdsForFaculty = studentService.getGroupIdsByFaculty(roleScope.facultyId(), preferredFullTime);
        }

        boolean applyFacultyFilter = isDekanat && groupIdsForFaculty != null;
        boolean applyStudyFormFilter = isDekanat && preferredFullTime != null;

        Collator ukrainianCollator = Collator.getInstance(new Locale("uk", "UA"));

        return groups.stream()
                .filter(group -> !applyFacultyFilter || groupIdsForFaculty.contains(group.getId()))
                .filter(group -> !applyStudyFormFilter || matchesPreferredForm(preferredFullTime, group))
                .map(group -> new GroupDTO(
                        group.getGroupCode(),
                        group.getSpecialty().getTitle(),
                        group.getCourse(),
                        group.getGroupNumber(),
                        group.getYear()
                ))
                .sorted(Comparator.comparing(GroupDTO::getGroupCode, ukrainianCollator))
                .collect(Collectors.toList());
    }

    private boolean matchesPreferredForm(Boolean preferredFullTime, StudentGroupEntity group) {
        if (preferredFullTime == null) {
            return true;
        }

        return studentService.determineGroupStudyForm(group)
                .map(preferredFullTime::equals)
                .orElse(false);
    }

    private RoleScope parseRoleScope(String rawRoleType) {
        if (rawRoleType == null || rawRoleType.isBlank()) {
            return new RoleScope(null, null);
        }

        String trimmed = rawRoleType.trim();
        String[] parts = trimmed.split(":", 2);

        Long facultyId = parseFacultyId(parts[0]);
        Boolean fullTime = null;
        if (parts.length > 1) {
            fullTime = parseStudyFormToken(parts[1]);
        }

        if (fullTime == null && parts.length == 1) {
            fullTime = parseStudyFormToken(parts[0]);
        }

        return new RoleScope(facultyId, fullTime);
    }

    private Long parseFacultyId(String token) {
        if (token == null) {
            return null;
        }

        String candidate = token.trim();
        if (candidate.isEmpty()) {
            return null;
        }

        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < candidate.length(); i++) {
            char ch = candidate.charAt(i);
            if (Character.isDigit(ch)) {
                digits.append(ch);
            } else if (!Character.isWhitespace(ch)) {
                break;
            }
        }

        if (digits.length() == 0) {
            return null;
        }

        try {
            return Long.valueOf(digits.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean parseStudyFormToken(String token) {
        if (token == null) {
            return null;
        }

        String normalized = token.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.equals("pt") || normalized.equals("part") || normalized.contains("part_time")
                || normalized.contains("part-time") || normalized.contains("zaoch")
                || normalized.contains("заоч") || normalized.contains("extramur")
                || normalized.contains("distance") || normalized.contains("вечір")) {
            return Boolean.FALSE;
        }

        if (normalized.equals("ft") || normalized.contains("full_time") || normalized.contains("full-time")
                || normalized.contains("fulltime") || normalized.contains("денн")
                || normalized.contains("day") || normalized.contains("денна")) {
            return Boolean.TRUE;
        }

        return null;
    }

    private record RoleScope(Long facultyId, Boolean fullTime) {
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