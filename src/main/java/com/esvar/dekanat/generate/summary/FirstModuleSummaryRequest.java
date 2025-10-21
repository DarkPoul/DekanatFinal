package com.esvar.dekanat.generate.summary;

/**
 * Input data for generating the first module summary report.
 *
 * @param groupId        identifier of the student group
 * @param semester       semester number for which the report is generated
 * @param controlType    name of the control method (e.g. "Перший модульний контроль")
 */
public record FirstModuleSummaryRequest(Long groupId, int semester, String controlType) {
}