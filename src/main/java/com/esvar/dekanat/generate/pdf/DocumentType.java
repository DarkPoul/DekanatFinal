package com.esvar.dekanat.generate.pdf;

import com.esvar.dekanat.generate.StudentModelToDocumentGenerate;

import java.util.Objects;

/**
 * Supported statement document types with type-specific wording.
 */
public enum DocumentType {
    CONTROL_WORK("control-work", "Відмітка про зарахування контрольної роботи («зараховано»)", false),
    COURSE_WORK("course-work", "Відмітка про зарахування курсової роботи", true),
    COURSE_PROJECT("course-project", "Відмітка про зарахування курсового проєкту", true);

    private final String outputSuffix;
    private final String markColumnHeader;
    private final boolean includeTeacherSignature;

    DocumentType(String outputSuffix, String markColumnHeader, boolean includeTeacherSignature) {
        this.outputSuffix = outputSuffix;
        this.markColumnHeader = markColumnHeader;
        this.includeTeacherSignature = includeTeacherSignature;
    }

    public String outputSuffix() {
        return outputSuffix;
    }

    public String markColumnHeader() {
        return markColumnHeader;
    }

    public boolean includeTeacherSignature() {
        return includeTeacherSignature;
    }

    /**
     * Resolve the displayed mark for the student row depending on document type.
     * For control work we use national mark text (e.g. "зараховано"), for course work/project
     * we fall back to numeric mark when the textual mark is empty.
     */
    public String resolveMark(StudentModelToDocumentGenerate student) {
        if (student == null) {
            return "";
        }
        String mark = Objects.toString(student.nationalMark(), "");
        if (mark.isBlank()) {
            mark = Objects.toString(student.mark(), "");
        }
        return mark;
    }
}
