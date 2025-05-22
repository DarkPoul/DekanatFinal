package com.esvar.dekanat.service;

import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.entity.SpecialtyEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import com.esvar.dekanat.repository.GroupRepository;
import com.esvar.dekanat.security.SecurityService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
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
                .anyMatch(a -> a.getAuthority().startsWith("ROLE_DEKANAT_"));
        String roleType   = securityService.getCurrentRoleType();
        Long facultyId;
        if (isDekanat) {
            facultyId = Long.valueOf(roleType);
        } else {
            facultyId = null;
        }

        // 3. Фільтруємо та мапимо
        return groups.stream()
                // тільки для методиста — фільтр по факультету
                .filter(group -> {
                    if (!isDekanat) {
                        return true;  // адміністратор бачить усе
                    }
                    // перевіряємо: є хоч один студент з потрібним faculty_id?
                    return studentService.getStudentByGroupId(group.getId()).stream()
                            .anyMatch(s -> s.getFaculty().getId().equals(facultyId));
                })
                .map(group -> new GroupDTO(
                        group.getGroupCode(),
                        group.getSpecialty().getTitle(),
                        group.getCourse(),
                        group.getGroupNumber(),
                        group.getYear()
                ))
                .collect(Collectors.toList());
    }



    public List<String> getAllStudentsForSelectedGroup(String groupSelectValue) {
        return studentService.getStudentByGroupId(groupRepository.findByGroupCode(groupSelectValue).getId()).stream()
                .map(student -> student.getSurname() + " " + student.getName() + " " + student.getPatronymic())
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

        System.out.println("groupCode = " + groupCode);
        return groupRepository.findIdByGroupCode(groupCode).orElse(null);
    }

    public StudentGroupEntity getGroupByTitle(String title){
        return groupRepository.findByGroupCode(title);
    }
}