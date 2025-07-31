package com.esvar.dekanat.utilites;

import com.esvar.dekanat.document.DocumentException;
import org.docx4j.Docx4J;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Utility class for converting DOCX documents to PDF using docx4j.
 */
public final class PdfConverterUtil {

    private PdfConverterUtil() {}

    /**
     * Convert DOCX file to PDF.
     *
     * @param docxPath path to input DOCX file
     * @param pdfPath  path to output PDF file
     */
    public static void convert(Path docxPath, Path pdfPath) {
        try (OutputStream os = new FileOutputStream(pdfPath.toFile())) {
            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(docxPath.toFile());
            Docx4J.toPDF(wordMLPackage, os);
        } catch (Docx4JException | IOException e) {
            throw new DocumentException("Failed to convert document to PDF", e);
        }
    }
}