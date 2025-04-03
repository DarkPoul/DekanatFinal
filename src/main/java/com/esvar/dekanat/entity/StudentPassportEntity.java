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
@Table(name = "student_passport")
public class StudentPassportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private StudentEntity student;

    @Column
    private String nameEng;

    @Column
    private String surnameEng;

    @Column(nullable = false, length = 100)
    private String nationality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender sex;

    @Column(nullable = false)
    private Date issueDate;

    @Column
    private String issuedBy;

    @Column(nullable = false)
    private Date expireDate;

    @Column(nullable = false, length = 10)
    private String series;

    @Column(nullable = false, unique = true, length = 20)
    private String number;

    @Column
    private String identificationNumber;

    @Column
    private String UnzrCode;

    @Column
    private Date birthdate;

    @Column
    private String edboNumberPhis;
    @Column
    private String edboNumberZdob;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudentPassportEntity that = (StudentPassportEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(student, that.student) &&
                Objects.equals(nameEng, that.nameEng) &&
                Objects.equals(surnameEng, that.surnameEng) &&
                Objects.equals(nationality, that.nationality) &&
                sex == that.sex &&
                Objects.equals(issueDate, that.issueDate) &&
                Objects.equals(issuedBy, that.issuedBy) &&
                Objects.equals(expireDate, that.expireDate) &&
                Objects.equals(series, that.series) &&
                Objects.equals(number, that.number) &&
                Objects.equals(identificationNumber, that.identificationNumber) &&
                Objects.equals(UnzrCode, that.UnzrCode) &&
                Objects.equals(birthdate, that.birthdate) &&
                Objects.equals(edboNumberPhis, that.edboNumberPhis) &&
                Objects.equals(edboNumberZdob, that.edboNumberZdob);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, student, nameEng, surnameEng, nationality, sex, issueDate, issuedBy,
                expireDate, series, number, identificationNumber, UnzrCode, birthdate,
                edboNumberPhis, edboNumberZdob
        );
    }
}

