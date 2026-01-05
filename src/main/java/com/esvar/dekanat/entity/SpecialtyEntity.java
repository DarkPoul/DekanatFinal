package com.esvar.dekanat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "specialty")
public class SpecialtyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 10)
    private String abbreviation;

    @Column(name = "is_technikum", nullable = false)
    private boolean technikum;

    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = false)
    private FacultyEntity faculty;

    @ManyToOne
    @JoinColumn(name = "edu_program_id")
    private EduProgramEntity eduProgram;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpecialtyEntity that = (SpecialtyEntity) o;
        return Objects.equals(id, that.id)
                && Objects.equals(title, that.title)
                && Objects.equals(abbreviation, that.abbreviation)
                && technikum == that.technikum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, abbreviation, technikum);
    }
}
