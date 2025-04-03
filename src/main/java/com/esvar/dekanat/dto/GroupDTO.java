package com.esvar.dekanat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GroupDTO {

    private String groupCode; // Повна назва групи
    private String specialtyAbbreviation; // Абревіатура спеціальності
    private int course; // Курс
    private int groupNumber; // Номер групи
    private int year; // Рік створення групи

    @Override
    public String toString() {
        return groupCode;
    }
}
