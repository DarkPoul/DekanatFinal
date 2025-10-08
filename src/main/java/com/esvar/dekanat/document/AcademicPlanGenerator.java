package com.esvar.dekanat.document;


import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Example generator for academic plan documents.
 */
@Component
public class AcademicPlanGenerator implements DocumentGenerator {

    public static final String NAME = "academicPlan";
    private static final String TEMPLATE = "uploads/academicPlan.docx";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getTemplatePath() {
        return TEMPLATE;
    }

    @Override
    public Map<String, Object> prepareContext(Object data) throws DocumentException {
        if (data instanceof Map<?, ?> map) {

            // unchecked cast, caller responsible for passing correct map

            return (Map<String, Object>) map;
        }

        throw new DocumentException("Expected Map<String,Object> as context");

    }
}