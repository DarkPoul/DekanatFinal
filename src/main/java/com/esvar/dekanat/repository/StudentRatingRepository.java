package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.StudentRatingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRatingRepository extends JpaRepository<StudentRatingEntity, Long> {

    @Query(value = """
        SELECT sr FROM StudentRatingEntity sr
            JOIN sr.student st
            LEFT JOIN StudentInfoEntity info ON info.student = st
        WHERE (:specialty IS NULL OR sr.specialty.abbreviation = :specialty)
          AND (:course IS NULL OR sr.course = :course)
          AND (:groupNumber IS NULL OR sr.group.groupNumber = :groupNumber)
          AND (:year IS NULL OR sr.group.year = :year)
          AND (:budget = false OR info.typeOfIndividual = 'Держбюджет')
          AND (:technikum = false OR sr.specialty.technikum = true)
        """,
            countQuery = """
        SELECT COUNT(sr) FROM StudentRatingEntity sr
            JOIN sr.student st
            LEFT JOIN StudentInfoEntity info ON info.student = st
        WHERE (:specialty IS NULL OR sr.specialty.abbreviation = :specialty)
          AND (:course IS NULL OR sr.course = :course)
          AND (:groupNumber IS NULL OR sr.group.groupNumber = :groupNumber)
          AND (:year IS NULL OR sr.group.year = :year)
          AND (:budget = false OR info.typeOfIndividual = 'Держбюджет')
          AND (:technikum = false OR sr.specialty.technikum = true)
        """)
    Page<StudentRatingEntity> searchRatings(
            @Param("specialty") String specialty,
            @Param("course") Integer course,
            @Param("groupNumber") Integer groupNumber,
            @Param("year") Integer year,
            @Param("technikum") boolean technikum,
            @Param("budget") boolean budget,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(sr) FROM StudentRatingEntity sr
            JOIN sr.student st
            LEFT JOIN StudentInfoEntity info ON info.student = st
        WHERE (:specialty IS NULL OR sr.specialty.abbreviation = :specialty)
          AND (:course IS NULL OR sr.course = :course)
          AND (:groupNumber IS NULL OR sr.group.groupNumber = :groupNumber)
          AND (:year IS NULL OR sr.group.year = :year)
          AND (:budget = false OR info.typeOfIndividual = 'Держбюджет')
          AND (:technikum = false OR sr.specialty.technikum = true)
        """)
    long countRatings(
            @Param("specialty") String specialty,
            @Param("course") Integer course,
            @Param("groupNumber") Integer groupNumber,
            @Param("year") Integer year,
            @Param("technikum") boolean technikum,
            @Param("budget") boolean budget
    );

    boolean existsByStudentId(Long studentId);
}
