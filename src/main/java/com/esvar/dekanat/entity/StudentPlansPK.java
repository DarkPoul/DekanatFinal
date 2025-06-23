package com.esvar.dekanat.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class StudentPlansPK extends StudentGroupEntity implements Serializable {
    private Long student;
    private Long plan;
}