package com.esvar.dekanat.generate;

import java.util.List;
public record DataModelForMC1(String facultyName, String specialityName, String courseNumber,
                              String groupName, String studyYear, String day, String month, String year,
                              String disciplineName, String semesterNumber, String controlTypeName,
                              String hours, String firstTeacher, String secondTeacher, String gradeTeacher,
                              List<StudentModelToDocumentGenerate> students) {
}
