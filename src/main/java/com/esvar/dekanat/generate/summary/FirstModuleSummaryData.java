package com.esvar.dekanat.generate.summary;

import java.util.List;

public record FirstModuleSummaryData(
        String groupName,
        String controlName,
        List<String> disciplines,
        List<FirstModuleSummaryRow> rows
) {
    public FirstModuleSummaryData {
        disciplines = List.copyOf(disciplines);
        rows = List.copyOf(rows);
    }
}
