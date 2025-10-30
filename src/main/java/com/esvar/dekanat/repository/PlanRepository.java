package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<PlansEntity, Long> {


    List<PlansEntity> findByFacultyAndDepartment(FacultyEntity faculty, DepartmentEntity department);
    List<PlansEntity> findByFacultyAndDepartmentAndSpecialty(FacultyEntity faculty, DepartmentEntity department, SpecialtyEntity specialty);
    @Query("""
            SELECT DISTINCT p FROM PlansEntity p
            JOIN p.groups g
            WHERE g = :group AND p.semester = :semester
            """)
    List<PlansEntity> findByGroupAndSemester(@Param("group") StudentGroupEntity group, @Param("semester") int semester);

    @Query("""
            SELECT DISTINCT p FROM PlansEntity p
            JOIN p.groups g
            WHERE p.faculty = :faculty
              AND p.department = :department
              AND p.specialty = :specialty
              AND g.course = :course
            """)
    List<PlansEntity> findByFacultyAndDepartmentAndSpecialtyAndGroup_Course(@Param("faculty") FacultyEntity faculty,
                                                                            @Param("department") DepartmentEntity department,
                                                                            @Param("specialty") SpecialtyEntity specialty,
                                                                            @Param("course") int course);

    @Query("""
            SELECT DISTINCT p FROM PlansEntity p
            JOIN p.groups g
            WHERE p.faculty = :faculty
              AND p.department = :department
              AND p.specialty = :specialty
              AND g.course = :course
              AND g.groupNumber = :groupNumber
            """)
    List<PlansEntity> findByFacultyAndDepartmentAndSpecialtyAndGroup_CourseAndGroup_GroupNumber(@Param("faculty") FacultyEntity faculty,
                                                                                                @Param("department") DepartmentEntity department,
                                                                                                @Param("specialty") SpecialtyEntity specialty,
                                                                                                @Param("course") int course,
                                                                                                @Param("groupNumber") int groupNumber);

    @Query("""
            SELECT DISTINCT p FROM PlansEntity p
            JOIN p.groups g
            WHERE p.faculty = :faculty
              AND p.department = :department
              AND p.specialty = :specialty
              AND g.course = :course
              AND g.groupNumber = :groupNumber
              AND p.discipline = :discipline
            """)
    List<PlansEntity> findByFacultyAndDepartmentAndSpecialtyAndGroup_CourseAndGroup_GroupNumberAndDiscipline(@Param("faculty") FacultyEntity faculty,
                                                                                                             @Param("department") DepartmentEntity department,
                                                                                                             @Param("specialty") SpecialtyEntity specialty,
                                                                                                             @Param("course") int course,
                                                                                                             @Param("groupNumber") int groupNumber,
                                                                                                             @Param("discipline") DisciplineEntity discipline);

    List<PlansEntity> findByFacultyAndDepartmentAndSpecialty_Abbreviation(
            FacultyEntity faculty,
            DepartmentEntity department,
            String specialtyAbbreviation
    );

    @Query("""
            SELECT DISTINCT p FROM PlansEntity p
            JOIN p.groups g
            WHERE p.faculty = :faculty
              AND p.department = :department
              AND p.specialty.abbreviation = :specialtyAbbreviation
              AND g.course = :course
            """)
    List<PlansEntity> findByFacultyAndDepartmentAndSpecialty_AbbreviationAndGroup_Course(
            @Param("faculty") FacultyEntity faculty,
            @Param("department") DepartmentEntity department,
            @Param("specialtyAbbreviation") String specialtyAbbreviation,
            @Param("course") int course
    );

    @Query("""
            SELECT DISTINCT p FROM PlansEntity p
            JOIN p.groups g
            WHERE p.faculty = :faculty
              AND p.department = :department
              AND p.specialty.abbreviation = :specialtyAbbreviation
              AND g.course = :course
              AND g.groupNumber = :groupNumber
            """)
    List<PlansEntity> findByFacultyAndDepartmentAndSpecialty_AbbreviationAndGroup_CourseAndGroup_GroupNumber(
            @Param("faculty") FacultyEntity faculty,
            @Param("department") DepartmentEntity department,
            @Param("specialtyAbbreviation") String specialtyAbbreviation,
            @Param("course") int course,
            @Param("groupNumber") int groupNumber
    );

    @Query("""
            SELECT DISTINCT p FROM PlansEntity p
            JOIN p.groups g
            WHERE p.faculty = :faculty
              AND p.department = :department
              AND p.specialty.abbreviation = :specialtyAbbreviation
              AND g.course = :course
              AND g.groupNumber = :groupNumber
              AND p.discipline.title = :disciplineTitle
            """)
    List<PlansEntity> findByFacultyAndDepartmentAndSpecialty_AbbreviationAndGroup_CourseAndGroup_GroupNumberAndDiscipline_Title(
            @Param("faculty") FacultyEntity faculty,
            @Param("department") DepartmentEntity department,
            @Param("specialtyAbbreviation") String specialtyAbbreviation,
            @Param("course") int course,
            @Param("groupNumber") int groupNumber,
            @Param("disciplineTitle") String disciplineTitle
    );

}
