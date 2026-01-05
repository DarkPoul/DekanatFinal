package com.esvar.dekanat.service;

import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.dto.RatingFilterOptions;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentRatingEntity;
import com.esvar.dekanat.repository.MarksRepository;
import com.esvar.dekanat.repository.StudentRatingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
@Service
public class RatingService {

    private final GroupService groupService;
    private final StudentRatingRepository ratingRepository;
    private final MarksRepository marksRepository;
    private final StudentService studentService;

    public RatingService(GroupService groupService,
                         StudentRatingRepository ratingRepository,
                         MarksRepository marksRepository,
                         StudentService studentService) {
        this.groupService = groupService;
        this.ratingRepository = ratingRepository;
        this.marksRepository = marksRepository;
        this.studentService = studentService;
    }

    public List<GroupDTO> getGroups() {
        return groupService.getGroupsDTO();
    }

    public List<String> getSpecialties() {
        return getGroups().stream()
                .map(GroupDTO::getSpecialtyAbbreviation)
                .distinct()
                .sorted()
                .toList();
    }

    public List<Integer> getCourses() {
        return getGroups().stream()
                .map(GroupDTO::getCourse)
                .distinct()
                .sorted()
                .toList();
    }

    public List<Integer> getGroupNumbers() {
        return getGroups().stream()
                .map(GroupDTO::getGroupNumber)
                .distinct()
                .sorted()
                .toList();
    }

    public List<Integer> getYears() {
        return groupService.getYears();
    }

    public RatingFilterOptions getFilterOptions() {
        return groupService.getRatingFilterOptions();
    }

    public Page<StudentRatingEntity> searchRatings(String specialty,
                                                   Integer course,
                                                   Integer group,
                                                   Integer year,
                                                   boolean technikum,
                                                   boolean budget,
                                                   Pageable pageable,
                                                   Sort sort) {
        Sort defaultSort = Sort.by(Sort.Order.desc("averageScore"), Sort.Order.asc("group.groupCode"));
        Sort effectiveSort = sort == null || sort.isUnsorted() ? defaultSort : sort;
        Pageable effectivePageable = pageable == null
                ? PageRequest.of(0, 50, effectiveSort)
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), effectiveSort);
        return ratingRepository.searchRatings(
                specialty,
                course,
                group,
                year,
                technikum,
                budget,
                effectivePageable
        );
    }

    public long countRatings(String specialty,
                             Integer course,
                             Integer group,
                             Integer year,
                             boolean technikum,
                             boolean budget) {
        return ratingRepository.countRatings(
                specialty,
                course,
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
        int total = 0;
        int sum = 0;
        int c3 = 0, c4 = 0, c5 = 0;
        for (MarksEntity m : marks) {
            int g = m.getFinalGrade();
            if (g < 1) {
                continue;
            }
            total++;
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

    @Transactional
    public void updateRatingsForStudents(Collection<StudentEntity> students) {
        if (students == null || students.isEmpty()) {
            return;
        }
        students.forEach(this::updateRatingForStudent);
    }

    @Transactional
    public void updateRatingsForStudentIds(Collection<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }
        studentService.getStudentsByIds(studentIds.stream().toList())
                .forEach(this::updateRatingForStudent);
    }

    @Transactional
    public void updateRatingsForGroup(Long groupId) {
        if (groupId == null) {
            return;
        }
        updateRatingsForStudents(studentService.getStudentByGroupId(groupId));
    }
}
