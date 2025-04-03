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
@Table(name = "student_education")
public class StudentEducationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private StudentEntity student;

    @Column
    private String typeOfDocument;

    @Column
    private int honors;

    @Column
    private String series;

    @Column
    private String number;

    @Column
    private Date dateOfIssue;

    @Column
    private String issuedBy;

    @Column
    private String issuedByEng;

    @Column(name = "diploma_series")
    private String diplomaSeries;

    @Column(name = "diploma_number", unique = true)
    private String diplomaNumber;

    @Column
    private Date dateOfIssueDiploma;

    @Column
    private String numberOfDodatok;

    @Column
    private String themeOfWork;

    @Column String themeOfWorkEng;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudentEducationEntity that = (StudentEducationEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(student, that.student) &&
                Objects.equals(typeOfDocument, that.typeOfDocument) &&
                honors == that.honors &&
                Objects.equals(series, that.series) &&
                Objects.equals(number, that.number) &&
                Objects.equals(dateOfIssue, that.dateOfIssue) &&
                Objects.equals(issuedBy, that.issuedBy) &&
                Objects.equals(issuedByEng, that.issuedByEng) &&
                Objects.equals(diplomaSeries, that.diplomaSeries) &&
                Objects.equals(diplomaNumber, that.diplomaNumber) &&
                Objects.equals(dateOfIssueDiploma, that.dateOfIssueDiploma) &&
                Objects.equals(numberOfDodatok, that.numberOfDodatok) &&
                Objects.equals(themeOfWork, that.themeOfWork) &&
                Objects.equals(themeOfWorkEng, that.themeOfWorkEng);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, student, typeOfDocument, honors, series, number, dateOfIssue,
                issuedBy, issuedByEng, diplomaSeries, diplomaNumber, dateOfIssueDiploma,
                numberOfDodatok, themeOfWork, themeOfWorkEng
        );
    }
}