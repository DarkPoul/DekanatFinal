package com.esvar.dekanat.service;

import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.entity.SpecialtyEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import com.esvar.dekanat.repository.GroupRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupService {


    private final GroupRepository groupRepository;
    private final StudentService studentService;

    public GroupService(GroupRepository groupRepository, StudentService studentService) {
        this.groupRepository = groupRepository;
        this.studentService = studentService;
    }

    // Отримання всіх груп
    public List<StudentGroupEntity> getAllGroups() {
        return groupRepository.findAll();
    }

    public List<GroupDTO> getGroupsDTO() {
        return groupRepository.findAll().stream()
                .map(group -> new GroupDTO(
                        group.getGroupCode(), // Повна назва групи
                        group.getSpecialty().getTitle(), // Назва спеціальності
                        group.getCourse(), // Курс
                        group.getGroupNumber(), // Номер групи
                        group.getYear() // Рік створення групи
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