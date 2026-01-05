package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.FacultyEntity;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.SpecialtyEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import com.esvar.dekanat.entity.StudentRatingEntity;
import com.esvar.dekanat.repository.MarksRepository;
import com.esvar.dekanat.repository.StudentRatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private GroupService groupService;
    @Mock
    private StudentRatingRepository ratingRepository;
    @Mock
    private MarksRepository marksRepository;
    @Mock
    private StudentService studentService;

    private RatingService ratingService;

    @BeforeEach
    void setUp() {
        ratingService = new RatingService(groupService, ratingRepository, marksRepository, studentService);
    }

    @Test
    void recalculatesCountsAndAverageFromMarks() {
        FacultyEntity faculty = new FacultyEntity();
        faculty.setId(1L);

        SpecialtyEntity specialty = new SpecialtyEntity();
        specialty.setId(5L);
        specialty.setTitle("Тестова");
        specialty.setAbbreviation("TST");
        specialty.setTechnikum(false);
        specialty.setFaculty(faculty);

        StudentGroupEntity group = new StudentGroupEntity();
        group.setId(7L);
        group.setSpecialty(specialty);
        group.setCourse(2);
        group.setGroupNumber(4);
        group.setYear(2024);
        group.setGroupCode("TST-2-4-2024");

        StudentEntity student = new StudentEntity();
        student.setId(11L);
        student.setFaculty(faculty);
        student.setGroup(group);

        List<MarksEntity> marks = List.of(
                markWithGrade(95),
                markWithGrade(80),
                markWithGrade(65),
                markWithGrade(50),
                markWithGrade(0)
        );
        when(marksRepository.findByStudentId(student.getId())).thenReturn(marks);
        when(ratingRepository.findById(student.getId())).thenReturn(java.util.Optional.empty());
        when(ratingRepository.save(any(StudentRatingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ratingService.updateRatingForStudent(student);

        ArgumentCaptor<StudentRatingEntity> ratingCaptor = ArgumentCaptor.forClass(StudentRatingEntity.class);
        verify(ratingRepository).save(ratingCaptor.capture());
        StudentRatingEntity savedRating = ratingCaptor.getValue();

        assertEquals(new BigDecimal("72.50"), savedRating.getAverageScore());
        assertEquals(1, savedRating.getCount5());
        assertEquals(1, savedRating.getCount4());
        assertEquals(1, savedRating.getCount3());
        assertEquals(4, savedRating.getTotalSubjects());
        assertEquals(group, savedRating.getGroup());
        assertEquals(specialty, savedRating.getSpecialty());
    }

    private MarksEntity markWithGrade(int grade) {
        MarksEntity mark = new MarksEntity();
        mark.setFinalGrade(grade);
        return mark;
    }
}
