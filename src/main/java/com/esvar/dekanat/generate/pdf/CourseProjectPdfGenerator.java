package com.esvar.dekanat.generate.pdf;

import org.springframework.stereotype.Component;

/**
 * PDF generator for course project statements using the shared statement layout.
 */
@Component
public class CourseProjectPdfGenerator extends BaseStatementPdfGenerator {

    public static final String NAME = "course-project";

    public CourseProjectPdfGenerator() {
        super(DocumentType.COURSE_PROJECT);
    }

    @Override
    public String getName() {
        return NAME;
    }
}
