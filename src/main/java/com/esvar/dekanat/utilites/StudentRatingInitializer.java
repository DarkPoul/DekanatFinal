package com.esvar.dekanat.utilites;

import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentRatingEntity;
import com.esvar.dekanat.repository.StudentRatingRepository;
import com.esvar.dekanat.service.StudentService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

//@Component
public class StudentRatingInitializer implements ApplicationRunner {

    private final StudentRatingRepository ratingRepository;
    private final StudentService studentService;

    public StudentRatingInitializer(StudentRatingRepository ratingRepository,
                                    StudentService studentService) {
        this.ratingRepository = ratingRepository;
        this.studentService = studentService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<StudentEntity> students = studentService.getAllStudents();
        for (StudentEntity student : students) {
            if (ratingRepository.existsByStudentId(student.getId())) {
                continue;
            }

            StudentRatingEntity rating = new StudentRatingEntity();
            rating.setStudent(student);
            rating.setAverageScore(BigDecimal.ZERO);
            rating.setCount3(0);
            rating.setCount4(0);
            rating.setCount5(0);
            rating.setTotalSubjects(0);
            rating.setFaculty(student.getFaculty());
            rating.setSpecialty(student.getGroup().getSpecialty());
            rating.setCourse(student.getGroup().getCourse());
            rating.setGroup(student.getGroup());
            rating.setLastUpdated(new Timestamp(System.currentTimeMillis()));
            ratingRepository.save(rating);
        }
    }
}
