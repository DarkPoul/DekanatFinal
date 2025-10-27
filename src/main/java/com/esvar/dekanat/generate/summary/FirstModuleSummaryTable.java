package com.esvar.dekanat.generate.summary;

import com.esvar.dekanat.document.PdfGenerator;
import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.layout.element.Cell;

import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FirstModuleSummaryTable {
    public static void createHeaderTable(String groupName ,int totalColumns, Path pdfPath) throws IOException {

        Document doc;
        PdfFont font;
        try{
            Files.createDirectories(pdfPath.getParent());
            PdfWriter writer = new PdfWriter(pdfPath.toFile());
            PdfDocument pdfDoc = new PdfDocument(writer);
            try (InputStream fontStream = PdfGenerator.class.getResourceAsStream("/fonts/times.ttf")) {
                if (fontStream != null) {
                    FontProgram fp = FontProgramFactory.createFont(fontStream.readAllBytes());
                    font = PdfFontFactory.createFont(fp, PdfEncodings.IDENTITY_H);
                } else {
                    font = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
                }
            } catch (IOException e) {
                font = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
            }

            doc = new Document(pdfDoc, pdfDoc.getDefaultPageSize().rotate());
        } catch (Exception e){
            throw new IOException("Не вдалося створити PDF документ за шляхом: " + pdfPath, e);
        }


        doc.setFont(font);


        String title = "Зведений звіт результатів оцінювання студентів групи ВП-4-2-29 за перший модульний контроль";
        Paragraph header = new Paragraph(title)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setMarginBottom(20);
        doc.add(header);

        Table table = createEqualTable(8, 200f, 40f, 40f);

        table.addHeaderCell(new Cell()
                .add(new Paragraph("ПІБ"))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE));

// --- Інші заголовки вертикальні ---
        String[] headers = {
                "Дисципліна 1",
                "Дисципліна 2",
                "Історія сучасних технологій",
                "Алгоритми та структури даних",
                "Дисципліна 5",
                "Дисципліна 6",
                "Системне програмування",
                "Математичний аналіз"
        };
        for (String headerss : headers) {
            Paragraph rotated = new Paragraph(headerss)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setWidth(200f)
                    .setMultipliedLeading(0.9f)
                    // Поворот на 90 градусів
                    .setRotationAngle(Math.toRadians(90));
            Cell cell = new Cell()
                    .add(rotated)
                    .setWidth(40f)
                    .setHeight(200f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(cell);
        }

        Paragraph countZerosHeader = new Paragraph("Кількість нулів")
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setWidth(200f)
                .setMultipliedLeading(0.9f)
                .setRotationAngle(Math.toRadians(90));

        table.addHeaderCell(new Cell()
                .add(countZerosHeader)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE));

        // приклад кількох рядків
        for (int i = 1; i <= 3; i++) {
            table.addCell("Студент " + i);
            table.addCell("0");
            table.addCell("0");
            table.addCell("0");
            table.addCell("0");
            table.addCell("0");
            table.addCell("0");
            table.addCell("0");
            table.addCell("0");
            table.addCell("0");
        }

        table.addCell("Кількість нулів");
        table.addCell("0");
        table.addCell("0");
        table.addCell("0");
        table.addCell("0");
        table.addCell("0");
        table.addCell("0");
        table.addCell("0");
        table.addCell("0");
        table.addCell("0");

        doc.add(table);

        doc.close();



    }

    /**
     * Створює таблицю з однаковими середніми колонками
     *
     * @param middleCount  кількість середніх колонок
     * @param firstWidth   ширина першої колонки (pt)
     * @param middleWidth  ширина кожної середньої колонки (pt)
     * @param lastWidth    ширина останньої колонки (pt)
     * @return Table з фіксованими розмірами колонок
     */
    public static Table createEqualTable(int middleCount, float firstWidth, float middleWidth, float lastWidth) {
        // Загальна кількість колонок = перша + середні + остання
        int totalCols = middleCount + 2;
        float[] widths = new float[totalCols];

        widths[0] = firstWidth; // перша
        for (int i = 1; i <= middleCount; i++) {
            widths[i] = middleWidth; // усі середні однакові
        }
        widths[widths.length - 1] = lastWidth; // остання

        // створюємо таблицю з абсолютними розмірами
        Table table = new Table(widths);
        table.setWidth(UnitValue.createPointValue(
                firstWidth + middleCount * middleWidth + lastWidth
        ));

        return table;
    }
}
