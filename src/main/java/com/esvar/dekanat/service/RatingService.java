package com.esvar.dekanat.service;

import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentRatingEntity;
import com.esvar.dekanat.repository.MarksRepository;
import com.esvar.dekanat.repository.StudentRatingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.List;
@Service
public class RatingService {

    private final GroupService groupService;
    private final StudentRatingRepository ratingRepository;
    private final MarksRepository marksRepository;

    public RatingService(GroupService groupService, StudentRatingRepository ratingRepository, MarksRepository marksRepository) {
        this.groupService = groupService;
        this.ratingRepository = ratingRepository;
        this.marksRepository = marksRepository;
    }

    public List<String> getSpecialties() {
        return groupService.getGroupsDTO().stream()
                .map(GroupDTO::getGroupCode)
                .map(split -> split.split("-")[0])
                .distinct()
                .sorted()
                .toList();
    }

    public List<String> getCourses() {
        return groupService.getGroupsDTO().stream()
                .map(GroupDTO::getGroupCode)
                .map(split -> split.split("-")[1])
                .distinct()
                .sorted()
                .toList();
    }

    public List<String> getGroupCodes() {
        return groupService.getGroupsDTO().stream()
                .map(GroupDTO::getGroupCode)
                .map(split -> split.split("-")[2])
                .distinct()
                .sorted()
                .toList();
    }

    public List<String> getYears() {
            return groupService.getGroupsDTO().stream()
                    .map(GroupDTO::getGroupCode)
                    .map(split -> split.split("-")[3])
                    .distinct()
                    .sorted(Comparator.reverseOrder())
                    .toList();
    }

    public List<StudentRatingEntity> searchRatings(String specialty,
                                                   String course,
                                                   String group,
                                                   String year,
                                                   boolean technikum,
                                                   boolean budget) {
        Integer courseNumber = null;
        if (course != null && !course.isEmpty()) {
            try {
                courseNumber = Integer.parseInt(course);
            } catch (NumberFormatException ignored) {
            }
        }
        return ratingRepository.searchRatings(
                specialty,
                courseNumber,
                group,
                year,
                technikum,
                budget
        );
    }

    @Transactional
    public void updateRatingForStudent(StudentEntity student) {
        if (student == null) {
            return;
        }
        StudentRatingEntity rating = ratingRepository.findById(student.getId()).orElseGet(() -> {
            StudentRatingEntity r = new StudentRatingEntity();
            r.setStudent(student);
            r.setFaculty(student.getFaculty());
            r.setSpecialty(student.getGroup().getSpecialty());
            r.setCourse(student.getGroup().getCourse());
            r.setGroup(student.getGroup());
            return r;
        });

        List<MarksEntity> marks = marksRepository.findByStudentId(student.getId());
        int total = marks.size();
        int sum = 0;
        int c3 = 0, c4 = 0, c5 = 0;
        for (MarksEntity m : marks) {
            int g = m.getFinalGrade();
            sum += g;
            if (g >= 90) c5++;
            else if (g >= 74) c4++;
            else if (g >= 60) c3++;
        }

        BigDecimal avg = total > 0
                ? new BigDecimal(sum).divide(new BigDecimal(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        rating.setAverageScore(avg);
        rating.setCount3(c3);
        rating.setCount4(c4);
        rating.setCount5(c5);
        rating.setTotalSubjects(total);
        rating.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        ratingRepository.save(rating);
    }
}