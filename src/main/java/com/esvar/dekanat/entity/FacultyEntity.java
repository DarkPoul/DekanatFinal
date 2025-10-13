package com.esvar.dekanat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "students")
@Table(name = "faculty")
public class FacultyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String abbreviation;

    @Column(nullable = false, length = 255)
    private String title;

    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StudentEntity> students;

    @Column(nullable = false, length = 100)
    private String deanP;

    @Column(nullable = false, length = 100)
    private String deanI;

    @Column(nullable = false, length = 100)
    private String deanB;

    @Column(nullable = false, length = 100)
    private String deanLanding;
}
