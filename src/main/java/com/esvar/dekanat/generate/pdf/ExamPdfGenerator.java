package com.esvar.dekanat.generate.pdf;

import com.esvar.dekanat.generate.DataModelForZalik;
import org.springframework.stereotype.Component;

/**
 * PDF generator for exam control statements.
 */
@Component
public class ExamPdfGenerator extends BaseZalikStylePdfGenerator {

    public static final String NAME = "exam";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String outputSuffix(DataModelForZalik data) {
        return "exam";
    }
}
