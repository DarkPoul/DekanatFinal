package com.esvar.dekanat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

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

    // Поле зберігається як legacy, проте не використовується у новій логіці
    @Deprecated
    @ManyToOne
    @JoinColumn(name = "group_id", nullable = true)
    private StudentGroupEntity group;

    @ManyToMany
    @JoinTable(
            name = "group_plans",
            joinColumns = @JoinColumn(name = "plan_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<StudentGroupEntity> groups = new HashSet<>();

    @Column(name = "statement_number", nullable = false, length = 3)
    private String statementNumber;

    public void addGroup(StudentGroupEntity group) {
        if (group != null) {
            groups.add(group);
        }
    }

    public void removeGroup(StudentGroupEntity group) {
        if (group != null) {
            groups.remove(group);
        }
    }

    public void setGroups(Set<StudentGroupEntity> groups) {
        this.groups = groups == null ? new HashSet<>() : new HashSet<>(groups);
    }

    public Set<StudentGroupEntity> getGroups() {
        return groups;
    }
}

