package com.esvar.dekanat.generate.pdf;

import com.esvar.dekanat.generate.DataModelForZalik;
import org.springframework.stereotype.Component;

/**
 * PDF generator for differential credit statements.
 */
@Component
public class DifferentialZalikPdfGenerator extends BaseZalikStylePdfGenerator {

    public static final String NAME = "differential-zalik";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String outputSuffix(DataModelForZalik data) {
        return "differential-zalik";
    }
}
