package com.esvar.dekanat.generate;

/**
 * Student row for exam document generation.
 */
public record ExamStudentRow(
        int index,
        String surname,
        String name,
        String patronymic,
        String recordBookNumber,
        String knowledgeArea,
        String speciality,
        String firstModuleMark,
        String secondModuleMark,
        String finalControlMark,
        String totalPoints,
        String ectsGrade,
        String nationalGrade
) {}
