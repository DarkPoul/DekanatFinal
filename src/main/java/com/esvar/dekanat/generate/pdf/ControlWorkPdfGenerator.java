package com.esvar.dekanat.generate.pdf;

import org.springframework.stereotype.Component;

/**
 * PDF generator for control work statements.
 */
@Component
public class ControlWorkPdfGenerator extends BaseStatementPdfGenerator {

    public static final String NAME = "control-work";

    public ControlWorkPdfGenerator() {
        super(DocumentType.CONTROL_WORK);
    }

    @Override
    public String getName() {
        return NAME;
    }
}
