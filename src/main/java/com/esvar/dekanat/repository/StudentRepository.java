package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<StudentEntity, Long> {
    @Query("SELECT DISTINCT s.group.id FROM StudentEntity s WHERE s.faculty.id = :facultyId")
    List<Long> findDistinctGroupIdsByFacultyId(Long facultyId);

    List<StudentEntity> findByGroupId(long groupId);

    /**
     * Знайти студентів за списком ID.
     *
     * @param studentIds Список ID студентів.
     * @return List<StudentEntity> - список студентів.
     */
    List<StudentEntity> findByIdIn(List<Long> studentIds);

    StudentEntity findFirstBySurnameAndNameAndPatronymicOrderByIdAsc(String studentSurname, String studentName, String studentPatronymic);

    List<StudentEntity> findBySurnameIgnoreCaseAndNameIgnoreCaseOrderByIdAsc(String studentSurname, String studentName);

    StudentEntity findFirstBySurnameAndNameAndPatronymicAndGroupIdOrderByIdAsc(String studentSurname, String studentName, String studentPatronymic, long groupId);

    List<StudentEntity> findByGroup(StudentGroupEntity group);

    List<StudentEntity> findBySurnameIgnoreCaseAndNameIgnoreCaseAndGroup_GroupCodeOrderByIdAsc(String studentSurname, String studentName, String groupCode);

    StudentEntity findFirstBySurnameAndNameAndPatronymicAndGroup_GroupCodeOrderByIdAsc(String studentSurname, String studentName, String studentPatronymic, String groupCode);
}
