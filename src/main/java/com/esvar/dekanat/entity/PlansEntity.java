package com.esvar.dekanat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "plans")
public class PlansEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "specialty_id", nullable = false)
    private SpecialtyEntity specialty;

    @ManyToOne
    @JoinColumn(name = "discipline_id", nullable = false)
    private DisciplineEntity discipline;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private DepartmentEntity department;

    @Column(nullable = false)
    private int semester;

    @Column(nullable = false)
    private int hours;

    @Column(nullable = false)
    private boolean isElective;

    @Column(nullable = false)
    private int parts;

    @ManyToOne
    @JoinColumn(name = "first_control_id", nullable = false)
    private ControlMethodEntity firstControl;

    @ManyToOne
    @JoinColumn(name = "second_control_id", nullable = true)
    private ControlMethodEntity secondControl;

    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = false)
    private FacultyEntity faculty;

    //Приєднання групи до плана
    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroupEntity group;

}

