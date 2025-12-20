package com.esvar.dekanat.generate.pdf;

import com.esvar.dekanat.generate.DataModelForZalik;
import org.springframework.stereotype.Component;

/**
 * PDF generator for course work control statements.
 */
@Component
public class CourseWorkPdfGenerator extends BaseZalikStylePdfGenerator {

    public static final String NAME = "course-work";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String outputSuffix(DataModelForZalik data) {
        return "course-work";
    }
}
