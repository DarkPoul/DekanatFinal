package com.esvar.dekanat.document;

import com.esvar.dekanat.generate.StudentModelToDocumentGenerate;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
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

            Object studentsObj = variables.get("students");
            if (studentsObj instanceof List<?> list && !list.isEmpty()
                    && list.get(0) instanceof StudentModelToDocumentGenerate) {
                @SuppressWarnings("unchecked")
                List<StudentModelToDocumentGenerate> students = (List<StudentModelToDocumentGenerate>) studentsObj;
                fillStudentsTable(document, students);
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
        }  catch (RuntimeException e) {
        log.error("Unexpected error generating document", e);
        throw new DocumentException("Unexpected error", e);
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

    private void fillStudentsTable(XWPFDocument document, List<StudentModelToDocumentGenerate> students) {
        XWPFTable table = findStudentsTable(document);
        if (table == null) {
            log.warn("Students table not found in template");
            return;
        }

        int insertPos = 3;
        for (StudentModelToDocumentGenerate s : students) {
            XWPFTableRow row = table.insertNewTableRow(insertPos++);
            ensureCells(row, 8);
            int mark100 = parseIntSafe(s.mark());

            setCellText(row.getCell(0), String.valueOf(s.index()), ParagraphAlignment.CENTER);
            setCellText(row.getCell(1), s.name(), ParagraphAlignment.LEFT);
            setCellText(row.getCell(2), s.studentNumber(), ParagraphAlignment.CENTER);
            setCellText(row.getCell(3), convertMarkToNationalGrade(mark100), ParagraphAlignment.CENTER);
            setCellText(row.getCell(4), String.valueOf(mark100), ParagraphAlignment.CENTER);
            setCellText(row.getCell(5), convertMarkToECTSGrade(mark100), ParagraphAlignment.CENTER);
            setCellText(row.getCell(6), java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")), ParagraphAlignment.CENTER);
            setCellText(row.getCell(7), "", ParagraphAlignment.CENTER);
        }
    }

    private XWPFTable findStudentsTable(XWPFDocument document) {
        List<XWPFTable> tables = document.getTables();
        if (tables.size() >= 2) {
            return tables.get(1); // індекс 1 — це друга таблиця
        }
        return null;
    }

    private static void ensureCells(XWPFTableRow row, int count) {
        while (row.getTableCells().size() < count) {
            row.createCell();
        }
    }

    private static void setCellText(XWPFTableCell cell, String text, ParagraphAlignment align) {
        int pCount = cell.getParagraphs().size();
        for (int i = pCount - 1; i >= 0; i--) {
            cell.removeParagraph(i);
        }
        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setAlignment(align);
        paragraph.setVerticalAlignment(TextAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setFontSize(11);
        run.setFontFamily("Times New Roman");
        run.setBold(false);
        run.setText(text);
    }

    private static int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String convertMarkToNationalGrade(int mark) {
        if (mark >= 90) {
            return "Відмінно";
        } else if (mark >= 82) {
            return "Добре";
        } else if (mark >= 74) {
            return "Добре";
        } else if (mark >= 64) {
            return "Задовільно";
        } else if (mark >= 60) {
            return "Задовільно";
        } else if (mark >= 35) {
            return "Незадовільно";
        } else {
            return "Незадовільно";
        }
    }

    private static String convertMarkToECTSGrade(int mark) {
        if (mark >= 90) {
            return "A";
        } else if (mark >= 82) {
            return "B";
        } else if (mark >= 74) {
            return "C";
        } else if (mark >= 64) {
            return "D";
        } else if (mark >= 60) {
            return "E";
        } else if (mark >= 35) {
            return "FX";
        } else {
            return "F";
        }
    }


}