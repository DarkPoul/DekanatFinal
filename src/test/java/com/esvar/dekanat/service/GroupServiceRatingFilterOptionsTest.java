package com.esvar.dekanat.service;

import com.esvar.dekanat.dto.RatingFilterOptions;
import com.esvar.dekanat.entity.FacultyEntity;
import com.esvar.dekanat.entity.SpecialtyEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import com.esvar.dekanat.repository.GroupRepository;
import com.esvar.dekanat.security.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceRatingFilterOptionsTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private StudentService studentService;
    @Mock
    private SecurityService securityService;
    @Mock
    private FacultyService facultyService;

    private GroupService groupService;

    @BeforeEach
    void setUp() {
        groupService = new GroupService(groupRepository, studentService, securityService, facultyService);
    }

    @Test
    void methodistGetsOnlyOwnFacultyOptions() {
        FacultyEntity facultyOne = new FacultyEntity();
        facultyOne.setId(1L);
        SpecialtyEntity specialty = new SpecialtyEntity();
        specialty.setId(1L);
        specialty.setAbbreviation("SPEC");
        specialty.setTitle("Спеціальність");
        specialty.setTechnikum(false);
        specialty.setFaculty(facultyOne);

        StudentGroupEntity ownGroup = new StudentGroupEntity();
        ownGroup.setId(10L);
        ownGroup.setSpecialty(specialty);
        ownGroup.setCourse(2);
        ownGroup.setGroupNumber(3);
        ownGroup.setYear(2024);
        ownGroup.setGroupCode("SPEC-2-3-2024");

        SpecialtyEntity foreignSpecialty = new SpecialtyEntity();
        foreignSpecialty.setId(2L);
        foreignSpecialty.setAbbreviation("FOREIGN");
        foreignSpecialty.setTitle("Інша");
        foreignSpecialty.setTechnikum(false);
        StudentGroupEntity foreignGroup = new StudentGroupEntity();
        foreignGroup.setId(20L);
        foreignGroup.setSpecialty(foreignSpecialty);
        foreignGroup.setCourse(1);
        foreignGroup.setGroupNumber(1);
        foreignGroup.setYear(2023);
        foreignGroup.setGroupCode("FOREIGN-1-1-2023");

        when(groupRepository.findAll()).thenReturn(List.of(ownGroup, foreignGroup));
        when(studentService.getGroupIdsByFaculty(1L)).thenReturn(Set.of(ownGroup.getId()));

        User methodist = new User("user@example.com", "pwd",
                List.of(new SimpleGrantedAuthority("ROLE_DEKANAT")));
        when(securityService.getAuthenticatedUser()).thenReturn(methodist);
        when(securityService.getCurrentRoleType()).thenReturn("1");

        RatingFilterOptions options = groupService.getRatingFilterOptions();

        assertEquals(1, options.groups().size(), "Methodist should only see own faculty groups");
        assertIterableEquals(List.of("SPEC"), options.specialties(), "Only own specialty should be visible");
        assertIterableEquals(List.of(2), options.courses());
        assertIterableEquals(List.of(3), options.groupNumbers());
        assertIterableEquals(List.of(2024), options.years());
        assertEquals("SPEC", options.defaultSpecialty());
        assertEquals(2, options.defaultCourse());
        assertEquals(3, options.defaultGroupNumber());
        assertEquals(2024, options.defaultYear());
    }
}
