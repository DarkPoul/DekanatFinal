package com.esvar.dekanat.document;

import java.util.Map;

/**
 * Interface for document template generators.
 */
public interface DocumentGenerator {

    /**
     * Unique generator name used to lookup implementation.
     */
    String getName();

    /**
     * Path to template file.
     */
    String getTemplatePath();

    /**
     * Resolve template path for the provided data. By default returns {@link #getTemplatePath()}.
     *
     * @param data source object with generation data
     * @return path to template
     */
    default String resolveTemplatePath(Object data) {
        return getTemplatePath();
    }

    /**
     * Prepare context variables used in template.
     *
     * @param data source object with generation data
     * @return map of variables
     */
    Map<String, Object> prepareContext(Object data) throws DocumentException;
}
