package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentInfoEntity;
import com.esvar.dekanat.repository.StudentInfoRepository;
import org.springframework.stereotype.Service;

@Service
public class StudentInfoService {
    private final StudentInfoRepository studentInfoRepository;

    public StudentInfoService(StudentInfoRepository studentInfoRepository) {
        this.studentInfoRepository = studentInfoRepository;
    }


    public StudentInfoEntity getInfoByStudentModel(StudentEntity studentEntity) {
        return studentInfoRepository.findByStudentId(studentEntity.getId());
    }

    public void save(StudentInfoEntity studentInfoEntity) {
        studentInfoRepository.save(studentInfoEntity);
    }
}
