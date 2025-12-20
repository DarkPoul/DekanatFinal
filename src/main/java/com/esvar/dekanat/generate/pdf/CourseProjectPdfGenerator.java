package com.esvar.dekanat.generate.pdf;

import com.esvar.dekanat.generate.DataModelForZalik;
import org.springframework.stereotype.Component;

/**
 * PDF generator for course project control statements.
 */
@Component
public class CourseProjectPdfGenerator extends BaseZalikStylePdfGenerator {

    public static final String NAME = "course-project";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String outputSuffix(DataModelForZalik data) {
        return "course-project";
    }
}
