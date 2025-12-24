package com.esvar.dekanat.generate;

import java.time.LocalDate;

public record StudentModelToDocumentGenerate(int index, String name, String studentNumber, String nationalMark,
                                             String mark, String ectsMark, LocalDate date,
                                             String dateText) {
    public String teacherSignPlaceholder() {
        return "";
    }
}
