package com.esvar.dekanat.dto;

import java.util.List;

public record RatingFilterOptions(
        List<GroupDTO> groups,
        List<String> specialties,
        List<Integer> courses,
        List<Integer> groupNumbers,
        List<Integer> years,
        String defaultSpecialty,
        Integer defaultCourse,
        Integer defaultGroupNumber,
        Integer defaultYear
) {
}
