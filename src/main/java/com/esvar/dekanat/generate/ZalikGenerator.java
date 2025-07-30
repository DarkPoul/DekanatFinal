package com.esvar.dekanat.generate;

import org.apache.poi.xwpf.usermodel.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Generator for zalik documents based on a docx template.
 */
public class ZalikGenerator {

    public String generate(DataModelForZalik data) {
        String inputFilePath = "uploads/zalik.docx";
        String tempFilePath = "uploads/zalikTemp.docx";
        String finalFilePath = buildFinalFilePath(
                data.controlTypeName(),
                data.groupName(),
                data.day(),
                data.month(),
                data.year()
        );

        try (FileInputStream fis = new FileInputStream(inputFilePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            replaceTagsInDocument(document, data);

            try (FileOutputStream fos = new FileOutputStream(tempFilePath)) {
                document.write(fos);
                DocxUpdater.runJar("WordToDocxConverter.jar", tempFilePath, finalFilePath);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return finalFilePath;
    }

    private void replaceTagsInDocument(XWPFDocument document, DataModelForZalik data) {
        replaceTagsInParagraphs(document, data);
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    replaceTagsInParagraphs(cell, data);
                }
            }
        }
    }

    private void replaceTagsInParagraphs(IBody body, DataModelForZalik data) {
        for (XWPFParagraph paragraph : body.getParagraphs()) {
            for (XWPFRun run : paragraph.getRuns()) {
                String text = run.getText(0);
                if (text != null) {
                    text = text.replace("{facultyName}", data.facultyName())
                            .replace("{specialityName}", data.specialityName())
                            .replace("{courseNumber}", data.courseNumber())
                            .replace("{groupName}", data.groupName())
                            .replace("{studyYear}", data.studyYear())
                            .replace("{order}", data.order())
                            .replace("{day}", data.day())
                            .replace("{month}", data.month())
                            .replace("{year}", data.year())
                            .replace("{disciplineName}", data.disciplineName())
                            .replace("{sN}", data.semesterNumber())
                            .replace("{controlTypeName}", data.controlTypeName())
                            .replace("{h}", data.hours())
                            .replace("{f}", data.firstTeacher())
                            .replace("{s}", data.secondTeacher())
                            .replace("{dekan}", data.dean())
                            .replace("{dName}", data.departmentName())
                            .replace("{A}", data.a())
                            .replace("{B}", data.b())
                            .replace("{C}", data.c())
                            .replace("{D}", data.d())
                            .replace("{E}", data.e())
                            .replace("{Fx}", data.fx())
                            .replace("{F}", data.f())
                            .replace("{tI}", data.gradeTeacher());
                    run.setText(text, 0);
                }
            }
        }
    }

    private String buildFinalFilePath(String controlName, String groupName, String day, String month, String year) {
        String shortName = toShortControlName(controlName);
        String safeControl = shortName.replaceAll("\\s+", "_");
        String fileName = groupName + "_" + safeControl + "_" + day + "_" + month + "_" + year + ".pdf";
        return "uploads/" + fileName;
    }

    private String toShortControlName(String controlName) {
        return switch (controlName) {
            case "Перший модульний контроль" -> "Перший модуль";
            case "Другий модульний контроль" -> "Другий модуль";
            case "Залік" -> "Залік";
            case "Екзамен" -> "Екзамен";
            case "Диференційний залік" -> "Д.залік";
            case "Курсова робота" -> "КР";
            case "Курсовий проєкт" -> "КП";
            case "Розрахункова робота" -> "РР";
            case "Розрахунково-графічна робота" -> "РГР";
            default -> controlName;
        };
    }
}