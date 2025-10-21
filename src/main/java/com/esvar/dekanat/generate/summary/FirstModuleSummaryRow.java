package com.esvar.dekanat.generate.summary;

import java.util.List;

/**
 * Represents a single row in the first module summary table for a student.
 */
public record FirstModuleSummaryRow(String studentName, List<Integer> marks) {

    public FirstModuleSummaryRow {
        marks = List.copyOf(marks);
    }

    /**
     * Counts how many module marks are equal to zero for the student.
     *
     * @return zero mark count
     */
    public long zeroCount() {
        return marks.stream().filter(mark -> mark == null || mark == 0).count();
    }
}