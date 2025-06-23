package com.esvar.dekanat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "student_plans")
public class StudentPlansEntity {

    @EmbeddedId
    private StudentPlansPK id;

    @ManyToOne
    @MapsId("student") // ім'я поля з @Embeddable
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @ManyToOne
    @MapsId("plan")
    @JoinColumn(name = "plan_id", nullable = false)
    private PlansEntity plan;
}