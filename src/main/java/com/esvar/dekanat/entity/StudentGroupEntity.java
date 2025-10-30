package com.esvar.dekanat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Objects;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "student_group")
public class StudentGroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "specialty_id", nullable = false)
    private SpecialtyEntity specialty;

    @Column(nullable = false)
    private int course;

    @Column(nullable = false)
    private int groupNumber;

    @Column(nullable = false)
    private int year;

    @Column(unique = true, updatable = false)
    private String groupCode;

    @ManyToMany(mappedBy = "groups")
    @ToString.Exclude
    private Set<PlansEntity> plans = new HashSet<>();

    @PrePersist
    protected void generateGroupCode() {
        if (this.specialty != null) {
            Long eduProgramId = this.specialty.getEduProgram() != null
                    ? this.specialty.getEduProgram().getId()
                    : null;
            if (eduProgramId != null) {
                this.groupCode = String.format("%s-%d-%d-%d(%d)",
                        this.specialty.getAbbreviation(), this.course, this.groupNumber, this.year, eduProgramId);
            } else if (this.specialty.getId() != null) {
                this.groupCode = String.format("%s-%d-%d-%d(%d)",
                        this.specialty.getAbbreviation(), this.course, this.groupNumber, this.year, this.specialty.getId());
            } else {
                this.groupCode = String.format("%s-%d-%d-%d",
                        this.specialty.getAbbreviation(), this.course, this.groupNumber, this.year);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudentGroupEntity that = (StudentGroupEntity) o;
        return course == that.course &&
                groupNumber == that.groupNumber &&
                year == that.year &&
                Objects.equals(id, that.id) &&
                Objects.equals(specialty, that.specialty) &&
                Objects.equals(groupCode, that.groupCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, specialty, course, groupNumber, year, groupCode);
    }
}
