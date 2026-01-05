package com.esvar.dekanat.generate;

import java.util.List;

/**
 * Data model for generating zalik documents.
 */
public record DataModelForZalik(
        String facultyName,
        String specialityName,
        String courseNumber,
        String groupName,
        String studyYear,
        String order,
        String day,
        String month,
        String year,
        String disciplineName,
        String semesterNumber,
        String controlTypeName,
        String hours,
        String firstTeacher,
        String secondTeacher,
        String deanPosition,
        String deanName,
        String departmentName,
        String a,
        String b,
        String c,
        String d,
        String e,
        String fx,
        String f,
        String gradeTeacher,
        List<StudentModelToDocumentGenerate> students
) {}
