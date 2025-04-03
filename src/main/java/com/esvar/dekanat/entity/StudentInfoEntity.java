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
@Table(name = "student_info")
public class StudentInfoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private StudentEntity student;

    @Column(nullable = false, length = 100)
    private String address;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(length = 100)
    private String caseNumber;

    @Column(length = 100)
    private String formStudy;

    @Column(length = 100)
    private String degree;

    @Column(length = 100)
    private String entryRequirements;

    @Column(length = 100)
    private String typeOfIndividual;

    @Column(length = 100)
    private String contractNumber;

    @Column(length = 100)
    private String total;

    @Column(length = 100)
    private String benefits;

    @Column
    private String region;

    @Column(name = "city_index")
    private String index;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudentInfoEntity that = (StudentInfoEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(student, that.student) &&
                Objects.equals(address, that.address) &&
                Objects.equals(phone, that.phone) &&
                Objects.equals(email, that.email) &&
                Objects.equals(caseNumber, that.caseNumber) &&
                Objects.equals(formStudy, that.formStudy) &&
                Objects.equals(degree, that.degree) &&
                Objects.equals(entryRequirements, that.entryRequirements) &&
                Objects.equals(typeOfIndividual, that.typeOfIndividual) &&
                Objects.equals(contractNumber, that.contractNumber) &&
                Objects.equals(total, that.total) &&
                Objects.equals(benefits, that.benefits) &&
                Objects.equals(region, that.region) &&
                Objects.equals(index, that.index);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, student, address, phone, email, caseNumber, formStudy, degree,
                entryRequirements, typeOfIndividual, contractNumber, total, benefits,
                region, index
        );
    }
}
