package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentPassportEntity;
import com.esvar.dekanat.repository.StudentPassportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudentPassportService {
    private final StudentPassportRepository studentPassportRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(StudentPassportService.class);

    public StudentPassportService(StudentPassportRepository studentPassportRepository) {
        this.studentPassportRepository = studentPassportRepository;
    }


    public Optional<StudentPassportEntity> getPassportByStudentModel(StudentEntity studentEntity) {
        List<StudentPassportEntity> passports = studentPassportRepository.findAllByStudent_Id(studentEntity.getId());
        if (passports.size() > 1) {
            LOGGER.warn("Found {} passport records for student with id {}. Returning the most recent entry.",
                    passports.size(),
                    studentEntity.getId());
        }
        return passports.stream().findFirst();
    }

    public StudentPassportEntity save(StudentPassportEntity passportEntity) {
        Optional<StudentPassportEntity> existing = Optional.empty();
        if (passportEntity.getStudent() != null && passportEntity.getStudent().getId() != null) {
            existing = studentPassportRepository
                    .findAllByStudent_Id(passportEntity.getStudent().getId())
                    .stream()
                    .findFirst();
        }

        existing.ifPresent(existingEntity -> passportEntity.setId(existingEntity.getId()));
        return studentPassportRepository.save(passportEntity);
    }
}
