package com.esvar.dekanat.generate.pdf;

import com.esvar.dekanat.generate.DataModelForZalik;
import org.springframework.stereotype.Component;

/**
 * PDF generator for calculation work statements.
 */
@Component
public class CalculationWorkPdfGenerator extends BaseZalikStylePdfGenerator {

    public static final String NAME = "calculation-work";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String outputSuffix(DataModelForZalik data) {
        return "calculation-work";
    }
}
