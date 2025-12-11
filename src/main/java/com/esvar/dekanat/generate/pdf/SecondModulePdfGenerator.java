package com.esvar.dekanat.generate.pdf;

import com.esvar.dekanat.document.DocumentException;
import com.esvar.dekanat.document.PdfGenerator;
import com.esvar.dekanat.generate.DataModelForMC2;
import com.esvar.dekanat.generate.StudentModelToDocumentGenerate;
import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * PDF generator for second module control grade sheets using iText.
 */
@Component
public class SecondModulePdfGenerator implements PdfGenerator {

    public static final String NAME = "second-module-control";

    private static final Logger log = LoggerFactory.getLogger(SecondModulePdfGenerator.class);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Path generatePdf(Object data) {
        if (!(data instanceof DataModelForMC2 moduleData)) {
            throw new DocumentException("Expected DataModelForMC2 for second module control");
        }

        try {
            Path outputPath = resolveOutputPath(moduleData);
            Files.createDirectories(outputPath.getParent());

            try (PdfWriter writer = new PdfWriter(outputPath.toFile());
                 PdfDocument pdfDocument = new PdfDocument(writer);
                 Document document = new Document(pdfDocument, PageSize.A4)) {

                PdfFont regular = loadFont("/fonts/times.ttf", StandardFonts.TIMES_ROMAN);
                PdfFont bold = loadFont("/fonts/timesbd.ttf", StandardFonts.TIMES_BOLD);

                document.setFont(regular);

                addHeader(document, regular, bold, moduleData);
                addStudentsTable(document, regular, moduleData.students());
                addSignatureSection(document, regular, moduleData);

                log.info("Generated second module control PDF at {}", outputPath);
            }

            return outputPath;
        } catch (IOException e) {
            throw new DocumentException("Failed to generate second module control PDF", e);
        }
    }

    private void addHeader(Document document, PdfFont regular, PdfFont bold, DataModelForMC2 data) {
        document.add(new Paragraph("НАЦІОНАЛЬНИЙ ТРАНСПОРТНИЙ УНІВЕРСИТЕТ")
                .setFont(bold)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER));

        SolidLine solidLine = new SolidLine(1f);
        LineSeparator line = new LineSeparator(solidLine);
        line.setMarginTop(-6);
        line.setMarginBottom(0);

        document.add(line);
        document.add(new Paragraph(Objects.toString(data.facultyName(), ""))
                .setFont(regular)
                .setFontSize(11));
        document.add(line);
        document.add(new Paragraph("Спеціальність: " + Objects.toString(data.specialityName(), ""))
                .setFont(regular)
                .setFontSize(11));
        document.add(line);

        Table groupTable = new Table(UnitValue.createPercentArray(new float[]{25, 10, 25, 40}))
                .useAllAvailableWidth();

        groupTable.addCell(new Cell().setPadding(0)
                .add(new Paragraph("Курс: ").setFont(regular).setFontSize(11))
                .setBorder(Border.NO_BORDER));
        groupTable.addCell(new Cell().setPadding(0)
                .add(new Paragraph(Objects.toString(data.courseNumber(), ""))
                        .setFont(regular)
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(0.5f)));
        groupTable.addCell(new Cell().setPadding(0)
                .add(new Paragraph("\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0Група: ")
                        .setFont(regular)
                        .setFontSize(11))
                .setBorder(Border.NO_BORDER));
        groupTable.addCell(new Cell().setPadding(0)
                .add(new Paragraph(Objects.toString(data.groupName(), ""))
                        .setFont(regular)
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(0.5f)));

        document.add(groupTable);

        document.add(new Paragraph(Objects.toString(data.studyYear(), "") + " навчальний рік")
                .setFont(regular)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph(Objects.toString(data.controlTypeName(), ""))
                .setFont(bold)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph(String.format("%s  %s  %s року", data.day(), data.month(), data.year()))
                .setFont(bold)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER)
                .setUnderline());

        Table disciplineTable = new Table(UnitValue.createPercentArray(new float[]{20, 60, 20}))
                .useAllAvailableWidth();
        disciplineTable.addCell(new Cell().setPadding(0)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("з дисципліни: ")
                        .setFont(regular)
                        .setFontSize(11)));
        disciplineTable.addCell(new Cell().setPadding(0)
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(0.5f))
                .add(new Paragraph(Objects.toString(data.disciplineName(), ""))
                        .setFont(regular)
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER)));
        disciplineTable.addCell(new Cell().setPadding(0)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("\u00A0")
                        .setFont(regular)
                        .setFontSize(11)));
        disciplineTable.addCell(new Cell().setPadding(0)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("\u00A0")
                        .setFont(regular)
                        .setFontSize(11)));
        disciplineTable.addCell(new Cell().setPadding(0)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("(назва дисципліни)")
                        .setFont(regular)
                        .setFontSize(8)
                        .setTextAlignment(TextAlignment.CENTER)));
        disciplineTable.addCell(new Cell().setPadding(0)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(String.format("Семестр: %s   Години: %s", data.semesterNumber(), data.hours()))
                        .setFont(regular)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.RIGHT)));

        document.add(disciplineTable);
    }

    private void addStudentsTable(Document document, PdfFont regular, List<StudentModelToDocumentGenerate> students) {
        float[] columns = {30, 200, 120, 60};
        Table table = new Table(UnitValue.createPointArray(columns))
                .useAllAvailableWidth();

        addHeaderCell(table, "№", regular);
        addHeaderCell(table, "Прізвище, ім'я та по батькові", regular);
        addHeaderCell(table, "Номер залікової", regular);
        addHeaderCell(table, "Оцінка", regular);

        for (StudentModelToDocumentGenerate student : students) {
            table.addCell(new Cell().add(new Paragraph(String.valueOf(student.index()))
                            .setFont(regular)
                            .setFontSize(10))
                    .setTextAlignment(TextAlignment.CENTER));
            table.addCell(new Cell().add(new Paragraph(student.name())
                            .setFont(regular)
                            .setFontSize(10))
                    .setTextAlignment(TextAlignment.LEFT));
            table.addCell(new Cell().add(new Paragraph(student.studentNumber())
                            .setFont(regular)
                            .setFontSize(10))
                    .setTextAlignment(TextAlignment.CENTER));
            table.addCell(new Cell().add(new Paragraph(student.mark())
                            .setFont(regular)
                            .setFontSize(10))
                    .setTextAlignment(TextAlignment.CENTER));
        }

        document.add(table);
    }

    private void addSignatureSection(Document document, PdfFont regular, DataModelForMC2 data) {
        document.add(new Paragraph("Якість знань (кількість студентів): ")
                .setFont(regular)
                .setFontSize(10));

        Table qualityTable = new Table(UnitValue.createPercentArray(new float[]{20, 20, 60}))
                .useAllAvailableWidth();
        qualityTable.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph("Позитивна: " + Objects.toString(data.qualityTrue(), ""))
                        .setFont(regular)
                        .setFontSize(10)));
        qualityTable.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph("Негативна: " + Objects.toString(data.qualityFalse(), ""))
                        .setFont(regular)
                        .setFontSize(10)));
        qualityTable.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(String.format("Викладач: %s", Objects.toString(data.gradeTeacher(), "")))
                        .setFont(regular)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.RIGHT)));

        document.add(qualityTable);

        document.add(new Paragraph(""));

        Table teachersTable = new Table(UnitValue.createPercentArray(new float[]{20, 30, 20, 30}))
                .useAllAvailableWidth();
        teachersTable.addCell(buildSignatureCell("Викладачі (М1):", regular));
        teachersTable.addCell(buildSignatureLine(data.firstTeacher(), regular));
        teachersTable.addCell(buildSignatureCell("Викладачі (М2):", regular));
        teachersTable.addCell(buildSignatureLine(data.secondTeacher(), regular));

        document.add(teachersTable);
    }

    private Cell buildSignatureCell(String title, PdfFont font) {
        return new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(title)
                        .setFont(font)
                        .setFontSize(10));
    }

    private Cell buildSignatureLine(String name, PdfFont font) {
        Paragraph paragraph = new Paragraph(Objects.toString(name, ""))
                .setFont(font)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setUnderline();
        return new Cell().setBorder(Border.NO_BORDER).add(paragraph);
    }

    private void addHeaderCell(Table table, String text, PdfFont font) {
        table.addCell(new Cell().add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(10)
                        .setBold())
                .setTextAlignment(TextAlignment.CENTER));
    }

    private PdfFont loadFont(String resourcePath, String fallbackFont) throws IOException {
        try (InputStream fontStream = SecondModulePdfGenerator.class.getResourceAsStream(resourcePath)) {
            if (fontStream != null) {
                FontProgram fontProgram = FontProgramFactory.createFont(fontStream.readAllBytes());
                return PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H);
            }
        } catch (IOException ex) {
            log.warn("Unable to load font {}: {}", resourcePath, ex.getMessage());
        }
        return PdfFontFactory.createFont(fallbackFont);
    }

    private Path resolveOutputPath(DataModelForMC2 data) {
        String shortControl = toShortControlName(Objects.toString(data.controlTypeName(), ""));
        String safeControl = shortControl.replaceAll("\\s+", "_");
        String fileName = Objects.toString(data.groupName(), "group") + "_" + safeControl + "_"
                + Objects.toString(data.day(), "00") + "_"
                + Objects.toString(data.month(), "00") + "_"
                + Objects.toString(data.year(), "0000") + ".pdf";
        return Paths.get("uploads").resolve(fileName);
    }

    private String toShortControlName(String controlName) {
        return switch (controlName) {
            case "Перший модульний контроль" -> "Перший модуль";
            case "Другий модульний контроль" -> "Другий модуль";
            default -> controlName == null ? "контроль" : controlName;
        };
    }
}
