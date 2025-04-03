package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentPassportEntity;
import com.esvar.dekanat.repository.StudentPassportRepository;
import org.springframework.stereotype.Service;

@Service
public class StudentPassportService {
    private final StudentPassportRepository studentPassportRepository;

    public StudentPassportService(StudentPassportRepository studentPassportRepository) {
        this.studentPassportRepository = studentPassportRepository;
    }


    public StudentPassportEntity getPassportByStudentModel(StudentEntity studentEntity) {
        return studentPassportRepository.findByStudentId(studentEntity.getId());
    }

    public void save(StudentPassportEntity passportEntity) {
        studentPassportRepository.save(passportEntity);
    }
}
