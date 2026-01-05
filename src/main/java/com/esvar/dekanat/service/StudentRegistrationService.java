package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.ReportEntity;
import com.esvar.dekanat.entity.StudentEducationEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentInfoEntity;
import com.esvar.dekanat.entity.StudentPassportEntity;
import com.esvar.dekanat.entity.StudentRatingEntity;
import com.esvar.dekanat.repository.StudentRatingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

@Service
public class StudentRegistrationService {

    private final StudentService studentService;
    private final StudentPassportService passportService;
    private final StudentInfoService infoService;
    private final StudentEducationService educationService;
    private final StudentRatingRepository ratingRepository;
    private final ReportService reportService;

    public StudentRegistrationService(StudentService studentService,
                                      StudentPassportService passportService,
                                      StudentInfoService infoService,
                                      StudentEducationService educationService,
                                      StudentRatingRepository ratingRepository,
                                      ReportService reportService) {
        this.studentService = studentService;
        this.passportService = passportService;
        this.infoService = infoService;
        this.educationService = educationService;
        this.ratingRepository = ratingRepository;
        this.reportService = reportService;
    }

    @Transactional
    public void saveStudentWithDetails(StudentEntity student,
                                       StudentPassportEntity passport,
                                       StudentInfoEntity info,
                                       StudentEducationEntity education,
                                       String reportStatus) {
        studentService.save(student);

        passport.setStudent(student);
        passportService.save(passport);

        info.setStudent(student);
        infoService.save(info);

        education.setStudent(student);
        educationService.save(education);

        ratingRepository.save(buildRating(student));

        if (reportStatus != null && !reportStatus.isBlank()) {
            ReportEntity report = new ReportEntity();
            report.setStudent(student);
            report.setStatus(reportStatus);
            report.setDate(new Date(System.currentTimeMillis()));
            report.setOrderNumber(reportService.getNextOrderNumber());
            reportService.saveReport(report);
        }
    }

    private StudentRatingEntity buildRating(StudentEntity student) {
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
        return rating;
    }
}
