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
 * PDF generator for second module control grade sheets.
 */
@Component
public class SecondModulePdfGenerator implements PdfGenerator {

    public static final String NAME = "second-module-control";

    private static final Logger log = LoggerFactory.getLogger(SecondModulePdfGenerator.class);
    private static final int PASSING_THRESHOLD = 19;

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
                addSemesterPerformanceSummary(document, regular, bold, moduleData.students());
                addSignatureSection(document, regular, bold, moduleData);

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
                .add(new Paragraph("\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0Група: ")
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

        Table disciplineTable = new Table(UnitValue.createPercentArray(new float[]{15, 70, 15}))
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
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(0.5f))
                .add(new Paragraph("")
                        .setFont(regular)
                        .setFontSize(11)));
        disciplineTable.addCell(new Cell().setPadding(0)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("")
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
                .add(new Paragraph("")
                        .setFont(regular)
                        .setFontSize(11)));

        document.add(disciplineTable);

        Table semControlTable = new Table(UnitValue.createPercentArray(new float[]{2, 5, 93}))
                .useAllAvailableWidth();
        semControlTable.addCell(new Cell().setPadding(0)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("за")
                        .setFont(regular)
                        .setFontSize(11)));
        semControlTable.addCell(new Cell().setPadding(0)
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(0.5f))
                .add(new Paragraph(Objects.toString(data.semesterNumber(), ""))
                        .setFont(regular)
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER)));
        semControlTable.addCell(new Cell().setPadding(0)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("навчальний семестр.")
                        .setFont(regular)
                        .setFontSize(11)));
        document.add(semControlTable);

        Table semesterControlTable = new Table(UnitValue.createPercentArray(new float[]{30, 30, 30, 10}))
                .useAllAvailableWidth();
        semesterControlTable.addCell(new Cell().setPadding(0)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("Форма семестрового контролю")
                        .setFont(regular)
                        .setFontSize(11)));
        semesterControlTable.addCell(new Cell().setPadding(0)
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(0.5f))
                .add(new Paragraph(Objects.toString(data.controlTypeName(), ""))
                        .setFont(regular)
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER)));
        semesterControlTable.addCell(new Cell().setPadding(0)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("Загальна кількість годин")
                        .setFont(regular)
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER)));
        semesterControlTable.addCell(new Cell().setPadding(0)
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(0.5f))
                .add(new Paragraph(Objects.toString(data.hours(), ""))
                        .setFont(regular)
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER)));
        document.add(semesterControlTable);

        document.add(createTeacherTable("Викладач", Objects.toString(data.firstTeacher(), ""), regular));
        document.add(createTeacherTable("Викладач", Objects.toString(data.secondTeacher(), ""), regular,
                "(прізвище, ім’я та по батькові викладача, який здійснював поточний контроль)"));

        document.add(new Paragraph("")
                .setFont(regular)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER));
    }

    private Table createTeacherTable(String label, String value, PdfFont font) {
        return createTeacherTable(label, value, font,
                "(прізвище, ім’я та по батькові викладача, який виставляє підсумкову оцінку)");
    }

    private Table createTeacherTable(String label, String value, PdfFont font, String hint) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{10, 80, 10}))
                .useAllAvailableWidth();
        table.addCell(new Cell().setPadding(0)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(label)
                        .setFont(font)
                        .setFontSize(11)));
        table.addCell(new Cell().setPadding(0)
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(0.5f))
                .add(new Paragraph(value)
                        .setFont(font)
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER)));
        table.addCell(new Cell().setPadding(0)
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(0.5f))
                .add(new Paragraph("")
                        .setFont(font)
                        .setFontSize(11)));
        table.addCell(new Cell().setPadding(0)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("")
                        .setFont(font)
                        .setFontSize(11)));
        table.addCell(new Cell().setPadding(0)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(hint)
                        .setFont(font)
                        .setFontSize(8)
                        .setTextAlignment(TextAlignment.CENTER)));
        table.addCell(new Cell().setPadding(0)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("")
                        .setFont(font)
                        .setFontSize(11)));
        return table;
    }

    private void addStudentsTable(Document document, PdfFont regular, List<StudentModelToDocumentGenerate> students) {
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{5, 35, 20, 20, 20}))
                .useAllAvailableWidth();

        headerTable.addHeaderCell(createHeaderCell("№\nз/п", regular));
        headerTable.addHeaderCell(createHeaderCell("Прізвище та ініціали студента", regular));
        headerTable.addHeaderCell(createHeaderCell("Номер залікової книжки", regular));
        headerTable.addHeaderCell(createHeaderCell("Сума балів за результатами першого та другого модулів (від 0 до 60 балів)", regular));
        headerTable.addHeaderCell(createHeaderCell("Підпис викладача", regular));

        document.add(headerTable);

        Table table = new Table(UnitValue.createPercentArray(new float[]{5, 35, 20, 20, 20}))
                .useAllAvailableWidth();

        for (StudentModelToDocumentGenerate student : students) {
            table.addCell(createBodyCell(String.valueOf(student.index()), regular, TextAlignment.CENTER));
            table.addCell(createBodyCell(student.name(), regular, TextAlignment.LEFT));
            table.addCell(createBodyCell(student.studentNumber(), regular, TextAlignment.CENTER));
            table.addCell(createBodyCell(student.mark(), regular, TextAlignment.CENTER));
            table.addCell(createBodyCell("", regular, TextAlignment.CENTER));
        }

        document.add(table);
    }

    private void addSemesterPerformanceSummary(Document document, PdfFont regular, PdfFont bold,
                                               List<StudentModelToDocumentGenerate> students) {
        document.add(new Paragraph("Підсумки семестрової успішності")
                .setFont(bold)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(8)
                .setMarginBottom(4));

        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .useAllAvailableWidth();
        summaryTable.addHeaderCell(createHeaderCell("Кількість студентів", regular));
        summaryTable.addHeaderCell(createHeaderCell("Сума балів", regular));

        SummaryCounts counts = calculateSummaryCounts(students);

        summaryTable.addCell(createBodyCell(String.valueOf(counts.aboveThreshold()), regular, TextAlignment.CENTER));
        summaryTable.addCell(createBodyCell("20-60", regular, TextAlignment.CENTER));

        summaryTable.addCell(createBodyCell(String.valueOf(counts.belowOrEqualThreshold()), regular, TextAlignment.CENTER));
        summaryTable.addCell(createBodyCell("0-19", regular, TextAlignment.CENTER));

        document.add(summaryTable);
    }

    private void addSignatureSection(Document document, PdfFont regular, PdfFont bold, DataModelForMC2 data) {
        document.add(new Paragraph("")
                .setFont(regular)
                .setFontSize(11)
                .setMarginTop(12)
                .setTextAlignment(TextAlignment.CENTER));

        Table signatureTable = new Table(UnitValue.createPercentArray(new float[]{20, 5, 20, 5, 20}))
                .useAllAvailableWidth();

        signatureTable.addCell(createSignatureLabelCell(bold));
        signatureTable.addCell(createSignatureSpacerCell());
        signatureTable.addCell(createSignatureLineCell(bold));
        signatureTable.addCell(createSignatureSpacerCell());
        signatureTable.addCell(createSignatureNameCell(resolveSignatureName(data), bold));

        signatureTable.addCell(createSignatureSpacerCell());
        signatureTable.addCell(createSignatureSpacerCell());
        signatureTable.addCell(createSignatureHintCell("(підпис)", regular));
        signatureTable.addCell(createSignatureSpacerCell());
        signatureTable.addCell(createSignatureHintCell("(прізвище,ініціали)", regular));

        document.add(signatureTable);
    }

    private Cell createSignatureLabelCell(PdfFont bold) {
        return new Cell().setPadding(0)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("Екзаменатор (Викладач)")
                        .setFont(bold)
                        .setFontSize(10));
    }

    private Cell createSignatureLineCell(PdfFont bold) {
        return new Cell().setPadding(0)
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(0.5f))
                .add(new Paragraph("")
                        .setFont(bold)
                        .setFontSize(10));
    }

    private Cell createSignatureNameCell(String examiner, PdfFont bold) {
        return new Cell().setPadding(0)
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(0.5f))
                .add(new Paragraph(Objects.toString(examiner, ""))
                        .setFont(bold)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER));
    }

    private Cell createSignatureHintCell(String text, PdfFont font) {
        return new Cell().setPadding(0)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(8)
                        .setTextAlignment(TextAlignment.CENTER));
    }

    private Cell createSignatureSpacerCell() {
        return new Cell().setPadding(0)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(""));
    }

    private String resolveSignatureName(DataModelForMC2 data) {
        if (!isBlank(data.secondTeacher())) {
            return data.secondTeacher();
        }
        if (!isBlank(data.gradeTeacher())) {
            return data.gradeTeacher();
        }
        if (!isBlank(data.firstTeacher())) {
            return data.firstTeacher();
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private SummaryCounts calculateSummaryCounts(List<StudentModelToDocumentGenerate> students) {
        int aboveThreshold = 0;
        int belowOrEqualThreshold = 0;

        for (StudentModelToDocumentGenerate student : students) {
            Integer mark = parseMark(student.mark());
            if (mark == null) {
                continue;
            }
            if (mark > PASSING_THRESHOLD) {
                aboveThreshold++;
            } else {
                belowOrEqualThreshold++;
            }
        }

        return new SummaryCounts(aboveThreshold, belowOrEqualThreshold);
    }

    private Integer parseMark(String mark) {
        if (mark == null) {
            return null;
        }
        try {
            return Integer.valueOf(mark.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Cell createHeaderCell(String text, PdfFont font) {
        return new Cell().add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(new SolidBorder(0.5f));
    }

    private Cell createBodyCell(String text, PdfFont font, TextAlignment alignment) {
        return new Cell().add(new Paragraph(Objects.toString(text, ""))
                        .setFont(font)
                        .setFontSize(10)
                        .setTextAlignment(alignment))
                .setBorder(new SolidBorder(0.5f));
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

    private record SummaryCounts(int aboveThreshold, int belowOrEqualThreshold) {
    }
}
