package com.esvar.dekanat.generate;

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
        String dean,
        String departmentName,
        String a,
        String b,
        String c,
        String d,
        String e,
        String fx,
        String f,
        String gradeTeacher
) {}