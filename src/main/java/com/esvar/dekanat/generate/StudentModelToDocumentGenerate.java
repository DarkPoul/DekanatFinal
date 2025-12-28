package com.esvar.dekanat.generate;

import java.time.LocalDate;

public record StudentModelToDocumentGenerate(int index, String name, String studentNumber, String nationalMark,
                                             String mark, String ectsMark, LocalDate date,
                                             String dateText, String teacherSignPlaceholder,
                                             String markText) {

    public StudentModelToDocumentGenerate {
        dateText = dateText == null ? "" : dateText;
        teacherSignPlaceholder = teacherSignPlaceholder == null ? "" : teacherSignPlaceholder;
        markText = markText == null ? "" : markText;
    }

    public StudentModelToDocumentGenerate(int index, String name, String studentNumber, String nationalMark,
                                          String mark, String ectsMark, LocalDate date, String dateText) {
        this(index, name, studentNumber, nationalMark, mark, ectsMark, date, dateText, "", "");
    }

    public StudentModelToDocumentGenerate(int index, String name, String studentNumber, String nationalMark,
                                          String mark, String ectsMark, LocalDate date, String dateText,
                                          String markText) {
        this(index, name, studentNumber, nationalMark, mark, ectsMark, date, dateText, "", markText);
    }
}
