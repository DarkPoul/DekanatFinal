package com.esvar.dekanat.generate;

import com.esvar.dekanat.document.DocumentException;
import com.esvar.dekanat.document.PdfGenerator;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * PDF generator for \"залікова відомість\" using iText7.
 */
@Component
public class ZalikPdfGenerator implements PdfGenerator {

    /**
     * Use the same generator name as the DOCX-based version so the
     * PDF generator transparently replaces it when present.
     */
    public static final String NAME = ZalikGenerator.NAME;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Path generatePdf(Object data) {
        if (!(data instanceof DataModelForZalik zalik)) {
            throw new DocumentException("Expected DataModelForZalik");
        }
        try {
            Path pdfPath = Files.createTempFile("zalik_", ".pdf");
            PdfWriter writer = new PdfWriter(pdfPath.toFile());
            PdfDocument pdfDoc = new PdfDocument(writer);

            PdfFont font;
            try {
                font = PdfFontFactory.createFont("/src/main/resources/fonts/times.ttf", PdfEncodings.IDENTITY_H);
            } catch (IOException e) {
                font = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
            }

            Document doc = new Document(pdfDoc);
            doc.setFont(font);

            doc.add(new Paragraph("Залікова відомість")
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            doc.add(new Paragraph("Факультет: " + zalik.facultyName()));
            doc.add(new Paragraph("Група: " + zalik.groupName()));
            doc.add(new Paragraph("Дисципліна: " + zalik.disciplineName()));

            float[] columnWidths = {30, 160, 80, 60, 40, 40, 60, 60};
            Table table = new Table(UnitValue.createPercentArray(columnWidths))
                    .useAllAvailableWidth();
            addHeaderCell(table, "№");
            addHeaderCell(table, "ПІБ");
            addHeaderCell(table, "Номер залікової");
            addHeaderCell(table, "Нац.");
            addHeaderCell(table, "Бали");
            addHeaderCell(table, "ECTS");
            addHeaderCell(table, "Дата");
            addHeaderCell(table, "Підпис");

            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            for (StudentModelToDocumentGenerate s : zalik.students()) {
                int mark100 = parseIntSafe(s.mark());
                table.addCell(new Cell().add(new Paragraph(String.valueOf(s.index())))
                        .setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(s.name())));
                table.addCell(new Cell().add(new Paragraph(s.studentNumber()))
                        .setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(convertMarkToNationalGrade(mark100)))
                        .setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(mark100)))
                        .setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(convertMarkToECTSGrade(mark100)))
                        .setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(LocalDate.now().format(df)))
                        .setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(""))
                        .setTextAlignment(TextAlignment.CENTER));
            }

            doc.add(table);
            doc.close();
            return pdfPath;
        } catch (IOException e) {
            throw new DocumentException("Failed to generate PDF", e);
        }
    }

    private static void addHeaderCell(Table table, String text) {
        table.addHeaderCell(new Cell().add(new Paragraph(text))
                .setTextAlignment(TextAlignment.CENTER));
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