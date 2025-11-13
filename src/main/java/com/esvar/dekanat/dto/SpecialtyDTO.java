package com.esvar.dekanat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SpecialtyDTO {

    private final String abbreviation;
    private final String title;

    @Override
    public String toString() {
        if (title != null && !title.isBlank()) {
            return title;
        }
        return abbreviation;
    }
}
