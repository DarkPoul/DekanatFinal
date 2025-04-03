package com.esvar.dekanat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "student_rating")
public class StudentRatingEntity {

    @Id
    @OneToOne
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @Column(name = "average_score", nullable = false)
    private BigDecimal averageScore;

    @Column(name = "count_3", nullable = false)
    private int count3;

    @Column(name = "count_4", nullable = false)
    private int count4;

    @Column(name = "count_5", nullable = false)
    private int count5;

    @Column(name = "total_subjects", nullable = false)
    private int totalSubjects;

    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = false)
    private FacultyEntity faculty;

    @ManyToOne
    @JoinColumn(name = "specialty_id", nullable = false)
    private SpecialtyEntity specialty;

    @Column(nullable = false)
    private int course;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroupEntity group;

    @Column(name = "last_updated", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Timestamp lastUpdated;
}