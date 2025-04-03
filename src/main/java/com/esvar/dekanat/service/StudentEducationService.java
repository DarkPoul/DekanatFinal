package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.StudentEducationEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.repository.StudentEducationRepository;
import org.springframework.stereotype.Service;

@Service
public class StudentEducationService {
    private final StudentEducationRepository studentEducationRepository;

    public StudentEducationService(StudentEducationRepository studentEducationRepository) {
        this.studentEducationRepository = studentEducationRepository;
    }

    public StudentEducationEntity getEducationByStudentModel(StudentEntity studentEntity) {
        return studentEducationRepository.findByStudentId(studentEntity.getId());
    }

    public void save(StudentEducationEntity educationEntity) {
        studentEducationRepository.save(educationEntity);
    }
}
