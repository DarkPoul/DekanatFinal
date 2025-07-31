package com.esvar.dekanat.document;

import java.nio.file.Path;

/**
 * Interface for generators that directly create PDF documents without a DOCX template.
 */
public interface PdfGenerator {
    /**
     * Unique generator name.
     */
    String getName();

    /**
     * Generate PDF using provided data and return path to the created file.
     */
    Path generatePdf(Object data) throws DocumentException;
}