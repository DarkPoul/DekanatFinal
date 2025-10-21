package com.esvar.dekanat.generate.summary;





import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.util.List;

/**
 * Utility responsible for rendering the first module summary table.
 */
public final class FirstModuleSummaryTable {

    private static final float NAME_COLUMN_WIDTH = 220f;
    private static final float DISCIPLINE_COLUMN_WIDTH = 45f;
    private static final float ZERO_COLUMN_WIDTH = 60f;

    private FirstModuleSummaryTable() {
    }

    public static void generate(Document doc, PdfFont font, FirstModuleSummaryData data) {
        doc.setFont(font);

        String title = String.format(
                "Зведений звіт результатів оцінювання студентів групи %s за %s",
                data.groupName(),
                data.controlName()
        );

        Paragraph header = new Paragraph(title)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setMarginBottom(20);
        doc.add(header);

        Table table = createEqualTable(data.disciplines().size(), NAME_COLUMN_WIDTH,
                DISCIPLINE_COLUMN_WIDTH, ZERO_COLUMN_WIDTH);

        table.addHeaderCell(createHeaderCell("ПІБ", false));

        for (String discipline : data.disciplines()) {
            table.addHeaderCell(createHeaderCell(discipline, true));
        }

        table.addHeaderCell(createHeaderCell("Кількість нулів", true));

        long[] zeroByDiscipline = new long[data.disciplines().size()];
        long totalZero = 0;

        for (FirstModuleSummaryRow row : data.rows()) {
            table.addCell(createBodyCell(row.studentName()));

            List<Integer> marks = row.marks();
            for (int i = 0; i < data.disciplines().size(); i++) {
                int value = i < marks.size() && marks.get(i) != null ? marks.get(i) : 0;
                if (value == 0) {
                    zeroByDiscipline[i]++;
                }
                table.addCell(createCenteredCell(Integer.toString(value)));
            }

            long zeros = row.zeroCount();
            totalZero += zeros;
            table.addCell(createCenteredCell(Long.toString(zeros)));
        }

        // Summary row with zero counts per discipline
        table.addCell(createSummaryCell("Кількість нулів"));
        for (long zeroCount : zeroByDiscipline) {
            table.addCell(createCenteredCell(Long.toString(zeroCount)));
        }
        table.addCell(createCenteredCell(Long.toString(totalZero)));

        doc.add(table);
    }

    private static Table createEqualTable(int middleCount, float firstWidth, float middleWidth, float lastWidth) {
        int totalCols = middleCount + 2;
        float[] widths = new float[totalCols];

        widths[0] = firstWidth;
        for (int i = 1; i <= middleCount; i++) {
            widths[i] = middleWidth;
        }
        widths[widths.length - 1] = lastWidth;

        Table table = new Table(widths);
        table.setWidth(UnitValue.createPointValue(firstWidth + middleCount * middleWidth + lastWidth));
        table.setFontSize(10);
        return table;
    }

    private static Cell createHeaderCell(String text, boolean rotate) {
        Paragraph paragraph = new Paragraph(text)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setMultipliedLeading(0.9f);
        if (rotate) {
            paragraph.setRotationAngle(Math.toRadians(90));
        }

        Cell cell = new Cell()
                .add(paragraph)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        if (rotate) {
            cell.setHeight(220f);
        }
        return cell;
    }

    private static Cell createBodyCell(String text) {
        return new Cell()
                .add(new Paragraph(text))
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private static Cell createCenteredCell(String value) {
        return new Cell()
                .add(new Paragraph(value))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private static Cell createSummaryCell(String text) {
        return new Cell()
                .add(new Paragraph(text))
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }
}