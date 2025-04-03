package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import com.esvar.dekanat.repository.GroupRepository;
import com.esvar.dekanat.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
        return studentRepository.findBySurnameAndNameAndPatronymicAndGroupId
                (
                        selectStudentValue.split(" ")[0],
                        selectStudentValue.split(" ")[1],
                        selectStudentValue.split(" ")[2],
                        groupRepository.findIdByGroupCode(selectGroupValue).orElseThrow()
                );
    }

    public List<StudentEntity> getStudentsForCard(String selectGroupValue) {
        return studentRepository.findByGroup(groupRepository.findByGroupCode(selectGroupValue));
    }

    public void save(StudentEntity studentEntity) {
        studentRepository.save(studentEntity);
    }

    public StudentEntity getStudentByStudentPIB_AndGroup(String studentPIB, StudentGroupEntity group) {
        return studentRepository.findBySurnameAndNameAndPatronymicAndGroup_GroupCode
                (
                        studentPIB.split(" ")[0],
                        studentPIB.split(" ")[1],
                        studentPIB.split(" ")[2],
                        group.getGroupCode()
        );
    }


}
