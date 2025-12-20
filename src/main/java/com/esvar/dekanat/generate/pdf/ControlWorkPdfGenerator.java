package com.esvar.dekanat.generate.pdf;

import com.esvar.dekanat.generate.DataModelForZalik;
import org.springframework.stereotype.Component;

/**
 * PDF generator for control work statements.
 */
@Component
public class ControlWorkPdfGenerator extends BaseZalikStylePdfGenerator {

    public static final String NAME = "control-work";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String outputSuffix(DataModelForZalik data) {
        return "control-work";
    }
}
