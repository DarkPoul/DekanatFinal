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
//            if (variables.containsKey("students")) {
//                Object obj = variables.get("students");
//                if (obj instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof StudentModelToDocumentGenerate) {
//                    @SuppressWarnings("unchecked")
//                    List<StudentModelToDocumentGenerate> students = (List<StudentModelToDocumentGenerate>) list;
//                    insertStudents(document, students);
//                }
//            }
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

//    private void insertStudents(XWPFDocument document, List<StudentModelToDocumentGenerate> students) {
//        XWPFTable table = findStudentsTable(document);
//        if (table == null || table.getNumberOfRows() < 3) {
//            log.warn("Students table not found or too small");
//            return;
//        }
//
//        // 1. Зчитати шаблонний рядок
//        XWPFTableRow templateRow = table.getRow(2);
//        CTRow templateCt = templateRow.getCtRow();
//
//        // 2. Видалити всі рядки нижче 2-го (починаючи з останнього)
//        for (int i = table.getNumberOfRows() - 1; i >= 2; i--) {
//            table.removeRow(i);
//        }
//
//        // 3. Створити рядки студентів
//        for (int i = 0; i < students.size(); i++) {
//            StudentModelToDocumentGenerate student = students.get(i);
//
//            log.info("Insert student [{}] - Name: {}, RecordBook: {}, Mark: {}",
//                    i + 1,
//                    student.name(),
//                    student.studentNumber(),
//                    student.mark());
//
//            // копія шаблонного рядка (нова щоразу!)
//            CTRow copiedCt = (CTRow) templateCt.copy(); // ключова зміна
//            XWPFTableRow row = new XWPFTableRow(copiedCt, table);
//            table.addRow(row);
//            ensureCells(row, 8);
//
//            if (row.getCell(0) == null || row.getCell(1) == null || row.getCell(2) == null || row.getCell(3) == null) {
//                log.error("One of the table cells is null at index {}", i);
//            }
//
//            setCellText(row.getCell(0), String.valueOf(i + 1));
//            setCellText(row.getCell(1), student.name());
//            setCellText(row.getCell(2), student.studentNumber());
//            setCellText(row.getCell(3), student.mark());
//            for (int c = 4; c < 8; c++) {
//                String mark = student.mark();
//                if (mark == null) {
//                    log.warn("Student [{}] has null mark, replacing with empty string", i + 1);
//                    mark = "";
//                }
//                setCellText(row.getCell(c), "");
//            }
//        }
//
//        // 4. Видалити шаблонний рядок після вставки
//        table.removeRow(2);
//    }

//    private static void ensureCells(XWPFTableRow row, int count) {
//        while (row.getTableCells().size() < count) {
//            row.createCell();
//        }
//    }
//
//    private static XWPFTableRow copyRowStyle(CTRow templateCtRow, XWPFTable table) {
//        CTRow ctRow = CTRow.Factory.newInstance();
//        ctRow.set(templateCtRow);
//        XWPFTableRow newRow = new XWPFTableRow(ctRow, table);
//        table.addRow(newRow);
//        return newRow;
//    }
//
//    private static void setCellText(XWPFTableCell cell, String text) {
//        int pCount = cell.getParagraphs().size();
//        for (int i = pCount - 1; i >= 0; i--) {
//            cell.removeParagraph(i);
//        }
//
//        XWPFParagraph paragraph = cell.addParagraph();
//        XWPFRun run = paragraph.createRun();
//        run.setFontFamily("Times New Roman");
//        run.setFontSize(11);
//        run.setBold(false);
//        run.setText(text);
//
//    }
//
//    private XWPFTable findStudentsTable(XWPFDocument document) {
//        for (XWPFTable tbl : document.getTables()) {
//            if (tbl.getNumberOfRows() < 1) {
//                continue;
//            }
//            XWPFTableRow header = tbl.getRow(0);
//            if (header.getTableCells().isEmpty()) {
//                continue;
//            }
//            String first = getCellPlainText(header.getCell(0)).toLowerCase();
//            String second = header.getTableCells().size() > 1 ? getCellPlainText(header.getCell(1)).toLowerCase() : "";
//            if (first.contains("№") && second.contains("прізвище")) {
//                return tbl;
//            }
//        }
//        return null;
//    }

//    private static String getCellPlainText(XWPFTableCell cell) {
//        StringBuilder sb = new StringBuilder();
//        for (XWPFParagraph p : cell.getParagraphs()) {
//            for (XWPFRun r : p.getRuns()) {
//                String t = r.getText(0);
//                if (t != null) {
//                    sb.append(t);
//                }
//            }
//        }
//        return sb.toString();
//    }
}