package com.esvar.dekanat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MarkDTO {

    private Long id; //орядковий номер

    private String studentPIB; //ПІБ студента

    private String MarkByFirstModule; //Оцінка за перший модуль

    private String enterMark; //Введення оцінки

    private String totalMarkByFirstAndSecondModule; //Сумарна оцінка за перший та другий модулі

    private String nationalGrade; //Національна оцінка

    private String ECTSGrade; //ECTS оцінка

    private String partMark1; //Оцінка за першу частину
    private String partMark2; //Оцінка за другу частину
    private String partMark3; //Оцінка за третю частину
    private String partMark4; //Оцінка за четверту частину
    private String partMark5; //Оцінка за п'яту частину
    private String partMark6; //Оцінка за шосту частину
    private String partMark7; //Оцінка за сьому частину
    private String partMark8; //Оцінка за восьму частину

    private String totalGrade; //Сумарна оцінка для РР або РГР

    private boolean isLocked;
    private String lastUpdated;
    private String lastUpdatedBy;

    @Override
    public String toString() {
        return "MarkDTO{" +
                "id=" + id +
                ", studentPIB='" + studentPIB + '\'' +
                ", MarkByFirstModule='" + MarkByFirstModule + '\'' +
                ", enterMark='" + enterMark + '\'' +
                ", totalMarkByFirstAndSecondModule='" + totalMarkByFirstAndSecondModule + '\'' +
                ", nationalGrade='" + nationalGrade + '\'' +
                ", ECTSGrade='" + ECTSGrade + '\'' +
                ", partMarks=[" + partMark1 + ", " + partMark2 + ", " + partMark3 + ", " + partMark4 +
                ", " + partMark5 + ", " + partMark6 + ", " + partMark7 + ", " + partMark8 + "]" +
                ", totalGrade='" + totalGrade + '\'' +
                ", isLocked=" + isLocked +
                ", lastUpdated='" + lastUpdated + '\'' +
                ", lastUpdatedBy='" + lastUpdatedBy + '\'' +
                '}';
    }

}
