package com.esvar.dekanat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(StudentPlansPK.class) // Використання складного первинного ключа
@Table(name = "student_plans")
public class StudentPlansEntity {

    @Id
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @Id
    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private PlansEntity plan;
}