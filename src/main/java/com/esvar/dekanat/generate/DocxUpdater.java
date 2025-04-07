package com.esvar.dekanat.generate;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHeightRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DocxUpdater {

    public DocxUpdater() {
    }

    public void generateForMC1(DataModelForMC1 data) {

        System.out.println("################################## START ##############################################");

        int i = 26; //Кількість студентів від яких йде весь розрахунок для перенесення таблиці на нову сторінку

        String inputFilePath = "uploads/firstControl.docx";
        String tempFilePath = "uploads/firstControlTemp.docx";
        String finalFilePath = "uploads/" + inputFilePath.split("/")[1].split("\\.")[0] + ".pdf";

        List<StudentModelToDocumentGenerate> students = data.students();

        try (FileInputStream fis = new FileInputStream(inputFilePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            replaceTagsInDocumentMC1(document, data);

            List<XWPFTable> tables = document.getTables();

            if (tables.size() < 2) {
                System.out.println("У документі немає другої таблиці.");
                return;
            }

            XWPFTable table = tables.get(1);


            for (StudentModelToDocumentGenerate student : students) {

                System.out.println("################################## START 2 ##############################################");

                if (students.size() <= i-3){ //якщо студентів менше ніж i - 3
                    XWPFTableRow newRow = table.createRow();
                    newRow.getCtRow().addNewTrPr().addNewTrHeight().setHRule(STHeightRule.EXACT);
                    newRow.getCtRow().getTrPr().getTrHeightArray(0).setVal(280);
                    fillStudentRow(newRow, student);
                }
                else if (students.size() > i + 2){ //якщо студентів більше ніж i - 3
                    if (student.index() == i + 3){ //створення другої таблиці при умові що індекс студента дорівнює максимальному
                        createTableWithInfo(document, tables, student);
                    } else if (student.index() < i + 3){ // заповнення таблиці при умові що 2-а таблиця ще не створена
                        XWPFTableRow newRow = table.createRow();
                        newRow.getCtRow().addNewTrPr().addNewTrHeight().setHRule(STHeightRule.EXACT);
                        newRow.getCtRow().getTrPr().getTrHeightArray(0).setVal(280);
                        fillStudentRow(newRow, student);
                    } else { // заповнення 2 таблиці
                        XWPFTable updateTable = document.getTables().get(2);
                        XWPFTableRow newRow = updateTable.createRow();
                        newRow.getCtRow().addNewTrPr().addNewTrHeight().setHRule(STHeightRule.EXACT);
                        newRow.getCtRow().getTrPr().getTrHeightArray(0).setVal(280);
                        fillStudentRow(newRow, student);
                    }

                }
                else {
                    if (student.index() == i - 2){
                        createTableWithInfo(document, tables, student);
                    } else if (student.index() < i - 2) {
                        XWPFTableRow newRow = table.createRow();
                        newRow.getCtRow().addNewTrPr().addNewTrHeight().setHRule(STHeightRule.EXACT);
                        newRow.getCtRow().getTrPr().getTrHeightArray(0).setVal(280);
                        fillStudentRow(newRow, student);
                    } else {
                        XWPFTable updateTable = document.getTables().get(2);
                        XWPFTableRow newRow = updateTable.createRow();
                        newRow.getCtRow().addNewTrPr().addNewTrHeight().setHRule(STHeightRule.EXACT);
                        newRow.getCtRow().getTrPr().getTrHeightArray(0).setVal(280);
                        fillStudentRow(newRow, student);
                    }
                }



            }
            tables = document.getTables();


            if (students.size() >= i + 2) {
                XmlCursor cursor3 = tables.get(3).getCTTbl().newCursor();
                document.insertNewParagraph(cursor3);
            } else {
                try{
                    XmlCursor cursor2 = tables.get(2).getCTTbl().newCursor();
                    if (students.size() > i - 3){
                        XWPFParagraph paragraph = document.insertNewParagraph(cursor2);
                        paragraph.setPageBreak(true);
                    } else {
                        document.insertNewParagraph(cursor2);
                    }
                } catch (IndexOutOfBoundsException ignored){}
                try{
                    XmlCursor cursor3 = tables.get(3).getCTTbl().newCursor();
                    document.insertNewParagraph(cursor3);
                } catch (IndexOutOfBoundsException ignored){}
            }


            try (FileOutputStream fos = new FileOutputStream(tempFilePath)) {
                System.out.println("word to pdf");
                document.write(fos);
                runJar("WordToDocxConverter.jar", tempFilePath, finalFilePath);
            }

            File file = new File(tempFilePath);
            if (file.exists()){
                if (file.delete()){
                    System.out.println("Тимчасовий файл видалено");
                } else System.out.println("Сталася помилка: тимчасовий файл не видалено");
            } else System.out.println("Тимчасовий файл не знайдено");

        } catch (IOException | InterruptedException e) {
            System.out.println("################################## START3 ##############################################");
            e.printStackTrace();
        }
    }
    public void generateForMC2(DataModelForMC2 date){

        int i = 26;

        String inputFilePath = "uploads/secondControl.docx";
        String tempFilePath = "uploads/secondControlTemp.docx" ;
        String finalFilePath = "uploads/" + inputFilePath.split("/")[1].split("\\.")[0] + ".pdf";

        List<StudentModelToDocumentGenerate> students = date.students();

        try (FileInputStream fis = new FileInputStream(inputFilePath);
             XWPFDocument document = new XWPFDocument(fis)){

            replaceTagsInDocumentMC2(document, date);

            List<XWPFTable> tables = document.getTables();
            XWPFTable table = tables.get(1);

            for (StudentModelToDocumentGenerate student : students){
                if (students.size() <= 14){
                    XWPFTableRow newRow = table.createRow();
                    newRow.getCtRow().addNewTrPr().addNewTrHeight().setHRule(STHeightRule.EXACT);
                    newRow.getCtRow().getTrPr().getTrHeightArray(0).setVal(280);
                    fillStudentRow(newRow, student);
                }
            }



            try (FileOutputStream fos = new FileOutputStream(tempFilePath)) {
                System.out.println("word to pdf");
                document.write(fos);
                runJar("WordToDocxConverter.jar", tempFilePath, finalFilePath.split("\\.")[0]  + "." + finalFilePath.split("\\.")[1]);
            }

            File file = new File(tempFilePath);
            if (file.exists()){
                if (file.delete()){
                    System.out.println("Тимчасовий файл видалено");
                } else System.out.println("Сталася помилка: тимчасовий файл не видалено");
            } else System.out.println("Тимчасовий файл не знайдено");

        } catch (IOException | InterruptedException e) {
            e.fillInStackTrace();
        }



    }

    private void createTableWithInfo(XWPFDocument document, List<XWPFTable> tables, StudentModelToDocumentGenerate student) {
        XWPFTable secondStudentsTable = document.createTable(1, 5);
        int pos = document.getPosOfTable(secondStudentsTable);
        document.removeBodyElement(pos);

        XmlCursor cursor = tables.get(2).getCTTbl().newCursor();


        XWPFTable newSecondStudentsTable = document.insertNewTbl(cursor);

        XWPFTableRow headerRow = newSecondStudentsTable.createRow();
        headerRow.getCtRow().addNewTrPr().addNewTrHeight().setHRule(STHeightRule.EXACT);
        headerRow.getCtRow().getTrPr().getTrHeightArray(0).setVal(280);

        for (int i = 0; i < 5; i++){
            if (headerRow.getCell(i) == null){
                XWPFTableCell cell = headerRow.createCell();
                styleCell(cell, String.valueOf((i+1)));
            } else {
                XWPFTableCell cell = headerRow.getCell(i);
                styleCell(cell, String.valueOf((i+1)));
            }

            switch (i){
                case 0:
                    headerRow.getCell(0).getCTTc().addNewTcPr().addNewTcW().setW(408);
                    break;
                case 1:
                    headerRow.getCell(1).getCTTc().addNewTcPr().addNewTcW().setW(3844);
                    break;
                case 2:
                    headerRow.getCell(2).getCTTc().addNewTcPr().addNewTcW().setW(1700);
                    break;
                case 3:
                    headerRow.getCell(3).getCTTc().addNewTcPr().addNewTcW().setW(1843);
                    break;
                case 4:
                    headerRow.getCell(4).getCTTc().addNewTcPr().addNewTcW().setW(1831);
                    break;
            }

        }

        newSecondStudentsTable.removeRow(0);

        XWPFTable updateTable = document.getTables().get(2);
        XWPFTableRow newRow = updateTable.createRow();
        newRow.getCtRow().addNewTrPr().addNewTrHeight().setHRule(STHeightRule.EXACT);
        newRow.getCtRow().getTrPr().getTrHeightArray(0).setVal(280);
        fillStudentRow(newRow, student);
    }

    private void fillStudentRow(XWPFTableRow row, StudentModelToDocumentGenerate student) {
        ensureCellExists(row, 0);
        ensureCellExists(row, 1);
        ensureCellExists(row, 2);
        ensureCellExists(row, 3);
        ensureCellExists(row, 4);

        setCellText(row.getCell(0), String.valueOf(student.index()), ParagraphAlignment.CENTER);
        setCellText(row.getCell(1), student.name(), ParagraphAlignment.LEFT);
        setCellText(row.getCell(2), student.studentNumber(), ParagraphAlignment.CENTER);
        setCellText(row.getCell(3), String.valueOf(student.mark()), ParagraphAlignment.CENTER);
        setCellText(row.getCell(4), "", ParagraphAlignment.CENTER); // Підпис викладача
    }

    private static void ensureCellExists(XWPFTableRow row, int cellIndex) {
        if (row.getCell(cellIndex) == null) {
            row.createCell();
        }
    }





    private void replaceTagsInDocumentMC1(XWPFDocument document, DataModelForMC1 data) {
        replaceTagsInParagraphsMC1(document, data);
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    replaceTagsInParagraphsMC1(cell, data);
                }
            }
        }
    }

    private static void replaceTagsInParagraphsMC1(IBody body, DataModelForMC1 data) {
        for (XWPFParagraph paragraph : body.getParagraphs()) {
            for (XWPFRun run : paragraph.getRuns()) {
                String text = run.getText(0);
                if (text != null) {
                    text = text.replace("{facultyName}", data.facultyName())
                            .replace("{specialityName}", data.specialityName())
                            .replace("{courseNumber}", data.courseNumber())
                            .replace("{groupName}", data.groupName())
                            .replace("{studyYear}", data.studyYear())
                            .replace("{day}", data.day())
                            .replace("{month}", data.month())
                            .replace("{year}", data.year())
                            .replace("{disciplineName}", data.disciplineName())
                            .replace("{sN}", data.semesterNumber())
                            .replace("{controlTypeName}", data.controlTypeName())
                            .replace("{h}", data.hours())
                            .replace("{f}", data.firstTeacher())
                            .replace("{s}", data.secondTeacher())
                            .replace("{tI}", data.gradeTeacher());
                    run.setText(text, 0);
                }
            }
        }
    }


    private void replaceTagsInDocumentMC2(XWPFDocument document, DataModelForMC2 data) {
        replaceTagsInParagraphsMC2(document, data);
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    replaceTagsInParagraphsMC2(cell, data);
                }
            }
        }
    }

    private static void replaceTagsInParagraphsMC2(IBody body, DataModelForMC2 data) {
        for (XWPFParagraph paragraph : body.getParagraphs()) {
            for (XWPFRun run : paragraph.getRuns()) {
                String text = run.getText(0);
                if (text != null) {
                    text = text.replace("{facultyName}", data.facultyName())
                            .replace("{specialityName}", data.specialityName())
                            .replace("{courseNumber}", data.courseNumber())
                            .replace("{groupName}", data.groupName())
                            .replace("{studyYear}", data.studyYear())
                            .replace("{day}", data.day())
                            .replace("{month}", data.month())
                            .replace("{year}", data.year())
                            .replace("{disciplineName}", data.disciplineName())
                            .replace("{sN}", data.semesterNumber())
                            .replace("{controlTypeName}", data.controlTypeName())
                            .replace("{h}", data.hours())
                            .replace("{f}", data.firstTeacher())
                            .replace("{s}", data.secondTeacher())
                            .replace("{qualityTrue}", data.qualityTrue())
                            .replace("{qualityFalse}", data.qualityFalse())
                            .replace("{tI}", data.gradeTeacher());
                    run.setText(text, 0);
                }
            }
        }
    }





    public static void runJar(String jarPath, String inputFilePath, String outputFilePath) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-jar");
        command.add(jarPath);
        command.add(inputFilePath);
        command.add(outputFilePath);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.inheritIO();
        Process process = processBuilder.start();

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            System.out.println("JAR файл виконано успішно.");
        } else {
            System.out.println("Сталася помилка під час виконання JAR файлу.");
        }
    }

    private static void setCellText(XWPFTableCell cell, String text, ParagraphAlignment align) {
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        paragraph.setAlignment(align);
        paragraph.setVerticalAlignment(TextAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setFontSize(10);
        run.setText(text);
    }

    private static void styleCell(XWPFTableCell cell, String text) {
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setVerticalAlignment(TextAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setFontSize(10);
        run.setBold(true);
        run.setItalic(false);
        run.setFontFamily("Times New Roman");
        run.setText(text);
    }
}
