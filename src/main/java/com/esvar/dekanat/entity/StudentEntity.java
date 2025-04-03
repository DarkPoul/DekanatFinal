package com.esvar.dekanat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.util.Objects;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "student")
public class StudentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String surname;

    @Column(length = 100)
    private String patronymic;

    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = false)
    private FacultyEntity faculty;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroupEntity group;

    @Column(unique = true, length = 50)
    private String recordBookNumber;

    public String getFullName() {
        System.out.println("surname: " + surname + " name: " + name + " patronymic: " + patronymic);
        return surname + " " + name + " " + patronymic;
    }

    public String getFullTitleGroup(){
        return group.getGroupCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudentEntity that = (StudentEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(surname, that.surname) &&
                Objects.equals(patronymic, that.patronymic) &&
                Objects.equals(faculty, that.faculty) &&
                Objects.equals(group, that.group) &&
                Objects.equals(recordBookNumber, that.recordBookNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, surname, patronymic, faculty, group, recordBookNumber);
    }
}

