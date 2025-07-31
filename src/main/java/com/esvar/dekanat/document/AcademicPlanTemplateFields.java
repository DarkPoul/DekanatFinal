package com.esvar.dekanat.document;

/**
 * Placeholders used in academic plan templates.
 */
public enum AcademicPlanTemplateFields {
    FACULTY_NAME("facultyName"),
    SPECIALITY_NAME("specialityName"),
    COURSE_NUMBER("courseNumber"),
    GROUP_NAME("groupName");

    private final String key;

    AcademicPlanTemplateFields(String key) {
        this.key = key;
    }

    /**
     * Key without braces used in templates.
     */
    public String key() {
        return key;
    }

    /**
     * Placeholder used inside document.
     */
    public String placeholder() {
        return '{' + key + '}';
    }
}