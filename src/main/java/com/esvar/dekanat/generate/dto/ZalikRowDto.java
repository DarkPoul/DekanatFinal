package com.esvar.dekanat.generate.dto;

import java.time.LocalDate;

/**
 * Row model for the zalik (exam) grade sheet.
 */
public record ZalikRowDto(
        int index,
        String name,
        String studentNumber,
        String nationalMark,
        String mark,
        String ectsMark,
        LocalDate date,
        String dateText,
        String teacherSignPlaceholder
) {

    public ZalikRowDto {
        teacherSignPlaceholder = teacherSignPlaceholder == null ? "" : teacherSignPlaceholder;
        dateText = dateText == null ? "" : dateText;
    }
}
