package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<PlansEntity, Long> {


    List<PlansEntity> findByFacultyAndDepartment(FacultyEntity faculty, DepartmentEntity department);
    List<PlansEntity> findByFacultyAndDepartmentAndSpecialty(FacultyEntity faculty, DepartmentEntity department, SpecialtyEntity specialty);
    List<PlansEntity> findByGroupAndSemester(StudentGroupEntity group, int semester);
    List<PlansEntity> findByFacultyAndDepartmentAndSpecialtyAndGroup_Course(FacultyEntity faculty, DepartmentEntity department, SpecialtyEntity specialty, int course);
    List<PlansEntity> findByFacultyAndDepartmentAndSpecialtyAndGroup_CourseAndGroup_GroupNumber(FacultyEntity faculty, DepartmentEntity department, SpecialtyEntity specialty, int course, int groupNumber);
    List<PlansEntity> findByFacultyAndDepartmentAndSpecialtyAndGroup_CourseAndGroup_GroupNumberAndDiscipline(FacultyEntity faculty, DepartmentEntity department, SpecialtyEntity specialty, int course, int groupNumber, DisciplineEntity discipline);

}
