package com.esvar.dekanat.generate.pdf;

import com.esvar.dekanat.document.DocumentException;
import com.esvar.dekanat.generate.DataModelForZalik;
import com.esvar.dekanat.generate.StudentModelToDocumentGenerate;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Normalized statement data used by PDF generators.
 */
public record StatementDocumentData(
        String facultyName,
        String specialityName,
        String courseNumber,
        String groupName,
        String studyYear,
        String sheetNumber,
        String day,
        String month,
        String year,
        String disciplineName,
        String semesterNumber,
        String controlTypeName,
        String teacherFullName,
        String headName,
        String headPosition,
        List<StudentModelToDocumentGenerate> students
) {

    public StatementDocumentData {
        students = students == null ? Collections.emptyList() : students;
    }

    public static StatementDocumentData from(Object source) {
        if (source instanceof StatementDocumentData statement) {
            return statement;
        }
        if (source instanceof DataModelForZalik zalik) {
            return new StatementDocumentData(
                    zalik.facultyName(),
                    zalik.specialityName(),
                    zalik.courseNumber(),
                    zalik.groupName(),
                    zalik.studyYear(),
                    zalik.order(),
                    zalik.day(),
                    zalik.month(),
                    zalik.year(),
                    zalik.disciplineName(),
                    zalik.semesterNumber(),
                    zalik.controlTypeName(),
                    zalik.firstTeacher(),
                    zalik.deanName(),
                    zalik.deanPosition(),
                    zalik.students()
            );
        }
        throw new DocumentException("Unsupported data type for statement PDF: " + source);
    }

    public String formattedDate() {
        String d = Objects.toString(day, "").trim();
        String m = Objects.toString(month, "").trim();
        String y = Objects.toString(year, "").trim();
        if (!d.isEmpty() && !m.isEmpty() && !y.isEmpty()) {
            return String.format("%s %s %s року", d, m, y);
        }
        return "";
    }
}
