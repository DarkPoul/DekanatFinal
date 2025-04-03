package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<StudentEntity, Long> {
    List<StudentEntity> findByGroupId(long groupId);

    /**
     * Знайти студентів за списком ID.
     *
     * @param studentIds Список ID студентів.
     * @return List<StudentEntity> - список студентів.
     */
    List<StudentEntity> findByIdIn(List<Long> studentIds);

    StudentEntity findBySurnameAndNameAndPatronymic(String studentSurname, String studentName, String studentPatronymic);

    StudentEntity findBySurnameAndNameAndPatronymicAndGroupId(String studentSurname, String studentName, String studentPatronymic, long groupId);

    List<StudentEntity> findByGroup(StudentGroupEntity group);

    StudentEntity findBySurnameAndNameAndPatronymicAndGroup_GroupCode(String studentSurname, String studentName, String studentPatronymic, String groupCode);
}
