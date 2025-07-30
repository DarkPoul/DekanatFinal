package com.esvar.dekanat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity that stores statement numbering for academic plans.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "plan_statement_number")
public class PlanStatementNumberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private PlansEntity academicPlan;

    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = false)
    private FacultyEntity faculty;

    @Column(name = "statement_number", nullable = false, length = 3)
    private String statementNumber;

    @Column(name = "control_type", nullable = false)
    private int controlType;

    @Column(name = "first_additional", nullable = false)
    private boolean firstAdditional;

    @Column(name = "second_additional", nullable = false)
    private boolean secondAdditional;
}