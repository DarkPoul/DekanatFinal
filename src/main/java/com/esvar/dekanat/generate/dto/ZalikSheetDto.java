package com.esvar.dekanat.generate.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Data transfer object for generating "ВІДОМІСТЬ ОБЛІКУ УСПІШНОСТІ" (залік/екзамен) PDF documents.
 */
public record ZalikSheetDto(
        String spec,
        String courseNumber,
        String groupName,
        String studyYear,
        String sheetNumber,
        LocalDate sheetDate,
        String sheetDay,
        String sheetMonth,
        String sheetYear,
        String disciplineName,
        int semestrNumber,
        String controlTypeName,
        int hours,
        String teacherFullName1,
        String teacherFullName2,
        String headPosition,
        String headName,
        String teacherInitials,
        List<ZalikRowDto> data,
        int countA,
        int countB,
        int countC,
        int countD,
        int countE,
        int countFx,
        int countF
) {

    public ZalikSheetDto {
        data = data == null ? List.of() : List.copyOf(data);
    }
}
