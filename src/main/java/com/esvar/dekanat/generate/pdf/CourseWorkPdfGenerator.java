package com.esvar.dekanat.generate.pdf;

import org.springframework.stereotype.Component;

/**
 * PDF generator for course work control statements.
 */
@Component
public class CourseWorkPdfGenerator extends BaseStatementPdfGenerator {

    public static final String NAME = "course-work";

    public CourseWorkPdfGenerator() {
        super(DocumentType.COURSE_WORK);
    }

    @Override
    public String getName() {
        return NAME;
    }
}
