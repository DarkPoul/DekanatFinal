package com.esvar.dekanat.generate;

/**
 * Summary information for exam document generation.
 */
public record ExamSummary(
        String countA,
        String countB,
        String countC,
        String countD,
        String countE,
        String countFx,
        String countF,
        String averagePoints,
        String averageNationalScore,
        String averageEctsScore,
        String totalStudents
) {}
