package com.esvar.dekanat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "marks_parts")
public class MarksPartsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "mark_id", nullable = false)
    private MarksEntity mark;

    @ManyToOne
    @JoinColumn(name = "part_id", nullable = false)
    private ControlPartsEntity controlPart;

    @Column(nullable = false)
    private BigDecimal grade;
}