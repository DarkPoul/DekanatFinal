package com.esvar.dekanat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(unique = true, updatable = false, insertable = false)
    private String groupCode;

    @PrePersist
    protected void generateGroupCode() {
        if (this.specialty != null) {
            this.groupCode = String.format("%s-%d-%d-%d",
                    this.specialty.getAbbreviation(), this.course, this.groupNumber, this.year);
        }
    }
}
