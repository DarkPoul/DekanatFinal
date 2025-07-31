package com.esvar.dekanat.document;

import com.esvar.dekanat.generate.StudentModelToDocumentGenerate;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Utility for generating documents from docx templates.
 */
public class DocumentTemplateEngine {

    private static final Logger log = LoggerFactory.getLogger(DocumentTemplateEngine.class);

    /**
     * Generate a document from the provided template and variables.
     *
     * @param templatePath path to the docx template
     * @param variables    map of placeholder variables
     * @return path to generated document
     */
    public Path generate(String templatePath, Map<String, Object> variables) {
        try (FileInputStream fis = new FileInputStream(templatePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            replaceTags(document, variables);
            if (variables.containsKey("students")) {
                Object obj = variables.get("students");
                if (obj instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof StudentModelToDocumentGenerate) {
                    @SuppressWarnings("unchecked")
                    List<StudentModelToDocumentGenerate> students = (List<StudentModelToDocumentGenerate>) list;
                    insertStudents(document, students);
                }
            }
            Path tempFile = Files.createTempFile("document_", ".docx");
            try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
                document.write(fos);
            }
            log.info("Document generated using template {}", templatePath);
            return tempFile;
        } catch (IOException e) {
            log.error("Error generating document", e);
            throw new DocumentException("Failed to generate document", e);
        }
    }

    private void replaceTags(IBody body, Map<String, Object> variables) {
        for (XWPFParagraph paragraph : body.getParagraphs()) {
            List<XWPFRun> runs = paragraph.getRuns();
            if (runs.isEmpty()) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            for (XWPFRun run : runs) {
                String text = run.getText(0);
                if (text != null) {
                    sb.append(text);
                }
            }
            String replaced = replace(sb.toString(), variables);
            // remove old runs and create a new one with replaced text
            for (int i = runs.size() - 1; i > 0; i--) {
                paragraph.removeRun(i);
            }
            runs.get(0).setText(replaced, 0);
        }
        if (body instanceof XWPFDocument doc) {
            for (XWPFTable table : doc.getTables()) {
                replaceInTable(table, variables);
            }
        } else if (body instanceof XWPFTableCell cell) {
            for (XWPFTable table : cell.getTables()) {
                replaceInTable(table, variables);
            }
        }
    }

    private void replaceInTable(XWPFTable table, Map<String, Object> vars) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                replaceTags(cell, vars);
            }
        }
    }

    private String replace(String text, Map<String, Object> variables) {
        String result = text;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = "{" + entry.getKey() + "}";
            Object value = entry.getValue();
            if (value != null) {
                result = result.replace(key, String.valueOf(value));
            }
        }
        return result;
    }

    private void insertStudents(XWPFDocument document, List<StudentModelToDocumentGenerate> students) {
        List<XWPFTable> tables = document.getTables();
        if (tables.size() < 2) {
            log.warn("Students table not found in template");
            return;
        }
        XWPFTable table = tables.get(1);
        for (StudentModelToDocumentGenerate student : students) {
            XWPFTableRow row = table.createRow();
            ensureCells(row, 8);
            row.getCell(0).setText(String.valueOf(student.index()));
            row.getCell(1).setText(student.name());
            row.getCell(2).setText(student.studentNumber());
            row.getCell(3).setText(student.mark());
            for (int i = 4; i < 8; i++) {
                row.getCell(i).setText("");
            }
        }
    }

    private static void ensureCells(XWPFTableRow row, int count) {
        while (row.getTableCells().size() < count) {
            row.createCell();
        }
    }
}