package com.esvar.dekanat.generate.summary;

import com.esvar.dekanat.document.PdfGenerator;
import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;

import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;


import org.springframework.stereotype.Component;

@Component
public class SummaryReportPdfGenerator {

    private static final float FIRST_COLUMN_WIDTH = 150f;
    private static final float OTHER_COLUMN_WIDTH = 35f;
    private static final float HEADER_ROW_HEIGHT = 110f;

    public byte[] generateSummaryReport(
            String groupName,
            List<String> studentFullNames,
            List<DisciplineColumn> disciplineColumns,
            Map<String, List<Integer>> marksByStudent,
            boolean addAverageRow
    ) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDocument = new PdfDocument(writer);
             Document document = new Document(pdfDocument, PageSize.A4.rotate())) {

            document.setMargins(20, 20, 20, 20);
            document.setFont(createFont());

            Paragraph title = new Paragraph(
                    "Зведений звіт результатів оцінювання студентів групи " + groupName +
                            " за перший модульний контроль")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(14)
                    .setMarginBottom(10);
            document.add(title);

            Table table = createTableStructure(disciplineColumns.size());
            addHeaderRows(table, disciplineColumns);
            addStudentRows(table, studentFullNames, disciplineColumns, marksByStudent);

            if (addAverageRow) {
                addZeroSummaryRow(table, studentFullNames, disciplineColumns, marksByStudent);
            }

            document.add(table);
        } catch (IOException e) {
            throw new IllegalStateException("Не вдалося згенерувати PDF звіт", e);
        }

        return baos.toByteArray();
    }

    private PdfFont createFont() throws IOException {
        try (InputStream fontStream = PdfGenerator.class.getResourceAsStream("/fonts/times.ttf")) {
            if (fontStream != null) {
                FontProgram fontProgram = FontProgramFactory.createFont(fontStream.readAllBytes());
                return PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H);
            }
        } catch (IOException ex) {
            // fall back to default font below
        }
        return PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
    }

    private Table createTableStructure(int disciplineCount) {
        int totalColumns = 2 + disciplineCount;
        float[] columnWidths = new float[totalColumns];
        columnWidths[0] = FIRST_COLUMN_WIDTH;
        for (int i = 1; i < totalColumns - 1; i++) {
            columnWidths[i] = OTHER_COLUMN_WIDTH;
        }
        columnWidths[totalColumns - 1] = OTHER_COLUMN_WIDTH;

        Table table = new Table(columnWidths);
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFixedLayout();
        return table;
    }

    private void addHeaderRows(Table table, List<DisciplineColumn> disciplineColumns) {
        int disciplineCount = disciplineColumns.size();

        Cell blankForName = new Cell().setBorder(new SolidBorder(1));
        table.addHeaderCell(blankForName);

        Cell disciplinesHeader = new Cell(1, disciplineCount)
                .add(new Paragraph("Дисципліна (макс. кількість балів)")
                        .setTextAlignment(TextAlignment.CENTER))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(1));
        table.addHeaderCell(disciplinesHeader);

        Cell blankForZeroColumn = new Cell().setBorder(new SolidBorder(1));
        table.addHeaderCell(blankForZeroColumn);

        Cell nameHeader = new Cell()
                .add(new Paragraph("ПІБ")
                        .setTextAlignment(TextAlignment.CENTER))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(1));
        table.addHeaderCell(nameHeader);

        for (DisciplineColumn discipline : disciplineColumns) {
            String headerText = discipline.title();
            if (discipline.elective()) {
                headerText = headerText + "\n(вибіркова)";
            }

            Paragraph rotated = new Paragraph(headerText)
                    .setRotationAngle(Math.toRadians(90))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10f);

            Cell disciplineHeaderCell = new Cell()
                    .add(rotated)
                    .setHeight(HEADER_ROW_HEIGHT)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setBorder(new SolidBorder(1));
            table.addHeaderCell(disciplineHeaderCell);
        }

        Paragraph zeroColumnText = new Paragraph("К-сть 0 у студента")
                .setRotationAngle(Math.toRadians(90))
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(10f);

        Cell zeroHeaderCell = new Cell()
                .add(zeroColumnText)
                .setHeight(HEADER_ROW_HEIGHT)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(1));
        table.addHeaderCell(zeroHeaderCell);
    }

    private void addStudentRows(Table table,
                                List<String> studentFullNames,
                                List<DisciplineColumn> disciplineColumns,
                                Map<String, List<Integer>> marksByStudent) {
        for (String student : studentFullNames) {
            Cell nameCell = new Cell()
                    .add(new Paragraph(student))
                    .setTextAlignment(TextAlignment.LEFT)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setBorder(new SolidBorder(1));
            table.addCell(nameCell);

            List<Integer> marks = marksByStudent.getOrDefault(student, Collections.emptyList());
            int zeroCount = 0;

            for (int i = 0; i < disciplineColumns.size(); i++) {
                Integer mark = getMark(marks, i);
                if (mark != null && mark == 0) {
                    zeroCount++;
                }

                table.addCell(createMarkCell(mark));
            }

            table.addCell(createNumericCell(zeroCount));
        }
    }

    private void addZeroSummaryRow(Table table,
                                   List<String> studentFullNames,
                                   List<DisciplineColumn> disciplineColumns,
                                   Map<String, List<Integer>> marksByStudent) {
        Cell labelCell = new Cell()
                .add(new Paragraph("Кількість 0 по дисциплінах"))
                .setTextAlignment(TextAlignment.LEFT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(1));
        table.addCell(labelCell);

        int totalZeros = 0;
        for (int i = 0; i < disciplineColumns.size(); i++) {
            int zeroPerDiscipline = 0;
            for (String student : studentFullNames) {
                List<Integer> marks = marksByStudent.getOrDefault(student, Collections.emptyList());
                Integer mark = getMark(marks, i);
                if (mark != null && mark == 0) {
                    zeroPerDiscipline++;
                }
            }
            totalZeros += zeroPerDiscipline;
            table.addCell(createNumericCell(zeroPerDiscipline));
        }

        table.addCell(createNumericCell(totalZeros));
    }

    private Cell createNumericCell(int value) {
        return new Cell()
                .add(new Paragraph(String.valueOf(value))
                        .setTextAlignment(TextAlignment.CENTER))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(1));
    }

    private Cell createMarkCell(Integer value) {
        if (value == null) {
            return createEmptyCell();
        }
        return createNumericCell(value);
    }

    private Cell createEmptyCell() {
        return new Cell()
                .add(new Paragraph(""))
                .setBorder(new SolidBorder(1));
    }

    private Integer getMark(List<Integer> marks, int index) {
        if (marks == null || index >= marks.size()) {
            return null;
        }
        return marks.get(index);
    }

    public record DisciplineColumn(String title, boolean elective) {
    }
}
