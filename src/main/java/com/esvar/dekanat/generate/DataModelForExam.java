package com.esvar.dekanat.generate;

import java.util.List;

/**
 * Data model for generating exam documents.
 */
public record DataModelForExam(
        String facultyName,
        String specialityName,
        String knowledgeArea,
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
        String departmentHead,
        String gradeTeacher,
        ExamSummary summary,
        List<ExamStudentRow> students
) {}
