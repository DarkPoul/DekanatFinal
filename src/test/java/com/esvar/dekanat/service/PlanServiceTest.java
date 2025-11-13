package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.*;
import com.esvar.dekanat.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;
    @Mock
    private StudentPlansRepository studentPlansRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private FacultyRepository facultyRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private MarksService marksService;
    @Mock
    private MarksPartsService marksPartsService;
    @Mock
    private MarksInitializerService marksInitializerService;
    @Mock
    private PlanStatementNumberService planStatementNumberService;

    @InjectMocks
    private PlanService planService;

    private FacultyEntity faculty;
    private DepartmentEntity department;
    private DisciplineEntity discipline;
    private ControlMethodEntity firstControl;
    private ControlMethodEntity secondControl;
    private StudentGroupEntity group;
    private PlansEntity plan;

    @BeforeEach
    void setUp() {
        faculty = new FacultyEntity();
        faculty.setId(1L);
        faculty.setTitle("Faculty");

        department = new DepartmentEntity();
        department.setId(2L);
        department.setTitle("Department");

        SpecialtyEntity specialty = new SpecialtyEntity();
        specialty.setId(3L);
        specialty.setAbbreviation("SP");

        discipline = new DisciplineEntity();
        discipline.setId(4L);
        discipline.setTitle("Math");

        firstControl = new ControlMethodEntity();
        firstControl.setId(5L);
        firstControl.setName("Екзамен");
        firstControl.setType(1);

        secondControl = new ControlMethodEntity();
        secondControl.setId(6L);
        secondControl.setName("Залік");
        secondControl.setType(2);

        group = new StudentGroupEntity();
        group.setId(7L);
        group.setCourse(1);
        group.setGroupNumber(1);
        group.setYear(2024);
        group.setSpecialty(specialty);
        group.setGroupCode("SP-1-1-2024");

        plan = new PlansEntity();
        plan.setId(8L);
        plan.setDiscipline(discipline);
        plan.setSemester(2);
        plan.setFirstControl(firstControl);
        plan.setSecondControl(secondControl);
        plan.setGroups(Set.of(group));

        when(facultyRepository.findByTitle("Faculty"))
                .thenReturn(faculty);
        when(departmentRepository.findByTitle("Department"))
                .thenReturn(department);
        when(sessionRepository.findById(1L))
                .thenReturn(Optional.of(new SessionEntity(1L, false)));
    }

    @Test
    void shouldIncludeModuleControlsForFullTimeGroup() {
        StudentEntity fullTimeStudent = createStudent(true);
        when(planRepository.findByFacultyAndDepartmentAndSpecialty_AbbreviationAndGroup_CourseAndGroup_GroupNumberAndDiscipline_Title(
                any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(List.of(plan));
        when(studentRepository.findByGroup(group))
                .thenReturn(List.of(fullTimeStudent));

        List<String> controlTypes = planService.getControlTypesByFacultyAndDepartmentAndSpecialtyAndGroupCourseAndGroupNumberAndDiscipline(
                "Faculty",
                "Department",
                "SP",
                1,
                1,
                "Math"
        );

        assertThat(controlTypes)
                .contains("Перший модульний контроль", "Другий модульний контроль");
    }

    @Test
    void shouldSkipModuleControlsForPartTimeGroup() {
        StudentEntity partTimeStudent = createStudent(false);
        when(planRepository.findByFacultyAndDepartmentAndSpecialty_AbbreviationAndGroup_CourseAndGroup_GroupNumberAndDiscipline_Title(
                any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(List.of(plan));
        when(studentRepository.findByGroup(group))
                .thenReturn(List.of(partTimeStudent));

        List<String> controlTypes = planService.getControlTypesByFacultyAndDepartmentAndSpecialtyAndGroupCourseAndGroupNumberAndDiscipline(
                "Faculty",
                "Department",
                "SP",
                1,
                1,
                "Math"
        );

        assertThat(controlTypes)
                .doesNotContain("Перший модульний контроль", "Другий модульний контроль");
    }

    private StudentEntity createStudent(boolean fullTime) {
        StudentEntity student = new StudentEntity();
        student.setId(fullTime ? 11L : 12L);
        student.setName("Ім'я");
        student.setSurname("Прізвище");
        student.setPatronymic("По батькові");
        student.setFaculty(faculty);
        student.setGroup(group);
        student.setRecordBookNumber(fullTime ? "FT" : "PT");
        student.setFullTime(fullTime);
        return student;
    }
}
