package com.esvar.dekanat.entity;

import com.esvar.dekanat.user.UserModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "marks")
public class MarksEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private PlansEntity plan;

    @ManyToOne
    @JoinColumn(name = "control_method_id", nullable = false)
    private ControlMethodEntity controlMethod;

    @Column(nullable = false)
    private int semester;

    @Column(precision = 5, scale = 2)
    private int finalGrade;

    @Column(nullable = false)
    private boolean isLocked;

    @Column(name = "last_updated", nullable = false)
    private Timestamp lastUpdated;

    @ManyToOne
    @JoinColumn(name = "last_updated_by")
    private UserModel lastUpdatedBy;
}

