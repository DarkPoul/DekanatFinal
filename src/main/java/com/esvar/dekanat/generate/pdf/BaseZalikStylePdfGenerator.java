package com.esvar.dekanat.generate.pdf;

import com.esvar.dekanat.document.DocumentException;
import com.esvar.dekanat.document.PdfGenerator;
import com.esvar.dekanat.generate.DataModelForZalik;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * Shared layout for Zalik-like control PDFs. Subclasses override only name and output suffix.
 */
public abstract class BaseZalikStylePdfGenerator implements PdfGenerator {

    private static final Logger log = LoggerFactory.getLogger(BaseZalikStylePdfGenerator.class);

    @Override
    public Path generatePdf(Object data) {
        if (!(data instanceof DataModelForZalik zalikData)) {
            throw new DocumentException("Expected DataModelForZalik");
        }

        try {
            Path outputPath = resolveOutputPath(zalikData);
            Files.createDirectories(outputPath.getParent());

            try (PdfWriter writer = new PdfWriter(outputPath.toFile());
                 PdfDocument pdfDocument = new PdfDocument(writer);
                 Document document = new Document(pdfDocument, PageSize.A4)) {

                PdfFont regular = loadFont("/fonts/times.ttf", StandardFonts.TIMES_ROMAN);
                PdfFont bold = loadFont("/fonts/timesbd.ttf", StandardFonts.TIMES_BOLD);

                document.setFont(regular);

                addHeader(document, regular, bold, zalikData);
                addStudentsTable(document, regular, bold, zalikData);
                addSummarySection(document, regular, bold, zalikData);

                log.info("Generated {} PDF at {}", getName(), outputPath);
            }

            return outputPath;
        } catch (IOException e) {
            throw new DocumentException("Failed to generate PDF", e);
        }
    }

    protected abstract String outputSuffix(DataModelForZalik data);

    private void addHeader(Document document, PdfFont regular, PdfFont bold, DataModelForZalik data) {
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

        groupTable.addCell(buildLabelCell("Курс: ", regular));
        groupTable.addCell(buildValueCell(Objects.toString(data.courseNumber(), ""), regular));
        groupTable.addCell(buildLabelCell("\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0Група: ", regular));
        groupTable.addCell(buildValueCell(Objects.toString(data.groupName(), ""), regular));

        document.add(groupTable);

        document.add(new Paragraph(Objects.toString(data.studyYear(), "") + " навчальний рік")
                .setFont(regular)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("ВІДОМІСТЬ ОБЛІКУ УСПІШНОСТІ № " + Objects.toString(data.order(), ""))
                .setFont(bold)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(6));

        document.add(new Paragraph(String.format("з дисципліни: %s за %s семестр",
                        Objects.toString(data.disciplineName(), ""), Objects.toString(data.semesterNumber(), "")))
                .setFont(regular)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Форма семестрового контролю: " + Objects.toString(data.controlTypeName(), ""))
                .setFont(regular)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Загальна кількість годин: " + Objects.toString(data.hours(), ""))
                .setFont(regular)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER));

        Table teachersTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth();
        teachersTable.addCell(buildTeacherCell("Перший викладач: ", Objects.toString(data.firstTeacher(), ""), regular));
        teachersTable.addCell(buildTeacherCell("Другий викладач: ", Objects.toString(data.secondTeacher(), ""), regular));

        document.add(teachersTable);
        document.add(new Paragraph(""));
    }

    private void addStudentsTable(Document document, PdfFont regular, PdfFont bold, DataModelForZalik data) {
        float[] columnWidths = {4, 28, 15, 14, 10, 10, 12, 7};
        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .useAllAvailableWidth();

        addHeaderCell(table, "№", bold);
        addHeaderCell(table, "ПІБ", bold);
        addHeaderCell(table, "Номер залікової книжки", bold);
        addHeaderCell(table, "Нац. оцінка", bold);
        addHeaderCell(table, "Бали", bold);
        addHeaderCell(table, "ECTS", bold);
        addHeaderCell(table, "Дата", bold);
        addHeaderCell(table, "Підпис", bold);

        String date = formatDate(data);
        List<StudentModelToDocumentGenerate> students = data.students();
        for (StudentModelToDocumentGenerate student : students) {
            int numericMark = parseToInt(student.mark());
            String nationalGrade = convertMarkToNationalGrade(numericMark);
            String ectsGrade = convertMarkToECTSGrade(numericMark);

            table.addCell(defaultCell(String.valueOf(student.index()), regular, TextAlignment.CENTER));
            table.addCell(defaultCell(student.name(), regular, TextAlignment.LEFT));
            table.addCell(defaultCell(student.studentNumber(), regular, TextAlignment.CENTER));
            table.addCell(defaultCell(nationalGrade, regular, TextAlignment.CENTER));
            table.addCell(defaultCell(student.mark(), regular, TextAlignment.CENTER));
            table.addCell(defaultCell(ectsGrade, regular, TextAlignment.CENTER));
            table.addCell(defaultCell(date, regular, TextAlignment.CENTER));
            table.addCell(defaultCell("", regular, TextAlignment.CENTER));
        }

        document.add(table);
    }

    private void addSummarySection(Document document, PdfFont regular, PdfFont bold, DataModelForZalik data) {
        document.add(new Paragraph(""));

        Table gradeTable = new Table(UnitValue.createPercentArray(new float[]{10, 10, 10, 10, 10, 10, 10}))
                .useAllAvailableWidth();
        addHeaderCell(gradeTable, "A", bold);
        addHeaderCell(gradeTable, "B", bold);
        addHeaderCell(gradeTable, "C", bold);
        addHeaderCell(gradeTable, "D", bold);
        addHeaderCell(gradeTable, "E", bold);
        addHeaderCell(gradeTable, "FX", bold);
        addHeaderCell(gradeTable, "F", bold);

        gradeTable.addCell(defaultCell(Objects.toString(data.a(), "0"), regular, TextAlignment.CENTER));
        gradeTable.addCell(defaultCell(Objects.toString(data.b(), "0"), regular, TextAlignment.CENTER));
        gradeTable.addCell(defaultCell(Objects.toString(data.c(), "0"), regular, TextAlignment.CENTER));
        gradeTable.addCell(defaultCell(Objects.toString(data.d(), "0"), regular, TextAlignment.CENTER));
        gradeTable.addCell(defaultCell(Objects.toString(data.e(), "0"), regular, TextAlignment.CENTER));
        gradeTable.addCell(defaultCell(Objects.toString(data.fx(), "0"), regular, TextAlignment.CENTER));
        gradeTable.addCell(defaultCell(Objects.toString(data.f(), "0"), regular, TextAlignment.CENTER));

        document.add(gradeTable);

        document.add(new Paragraph(""));

        Table signTable = new Table(UnitValue.createPercentArray(new float[]{33, 34, 33}))
                .useAllAvailableWidth();
        signTable.addCell(signatureCell("Викладач", Objects.toString(data.gradeTeacher(), ""), regular));
        signTable.addCell(signatureCell("Декан", Objects.toString(data.dean(), ""), regular));
        signTable.addCell(signatureCell("Завідувач кафедри", Objects.toString(data.departmentName(), ""), regular));

        document.add(signTable);
    }

    private Cell buildLabelCell(String text, PdfFont font) {
        return new Cell().setPadding(0)
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(11))
                .setBorder(Border.NO_BORDER);
    }

    private Cell buildValueCell(String text, PdfFont font) {
        return new Cell().setPadding(0)
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(0.5f));
    }

    private Cell buildTeacherCell(String label, String value, PdfFont font) {
        return new Cell().setPadding(4)
                .add(new Paragraph(label + value)
                        .setFont(font)
                        .setFontSize(11))
                .setBorder(Border.NO_BORDER);
    }

    private void addHeaderCell(Table table, String text, PdfFont bold) {
        table.addHeaderCell(new Cell().add(new Paragraph(text)
                        .setFont(bold)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER))
                .setPadding(4));
    }

    private Cell defaultCell(String text, PdfFont font, TextAlignment alignment) {
        return new Cell().add(new Paragraph(text == null ? "" : text)
                        .setFont(font)
                        .setFontSize(10)
                        .setTextAlignment(alignment))
                .setPadding(4);
    }

    private Cell signatureCell(String label, String value, PdfFont font) {
        Paragraph labelParagraph = new Paragraph(label)
                .setFont(font)
                .setFontSize(11);
        Paragraph valueParagraph = new Paragraph(value)
                .setFont(font)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorderBottom(new SolidBorder(0.5f));

        return new Cell().setPadding(6)
                .add(labelParagraph)
                .add(valueParagraph)
                .setBorder(Border.NO_BORDER);
    }

    private PdfFont loadFont(String resourcePath, String fallbackFont) throws IOException {
        try (InputStream fontStream = BaseZalikStylePdfGenerator.class.getResourceAsStream(resourcePath)) {
            if (fontStream != null) {
                FontProgram fontProgram = FontProgramFactory.createFont(fontStream.readAllBytes());
                return PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H);
            }
        } catch (IOException ex) {
            log.warn("Unable to load font {}: {}", resourcePath, ex.getMessage());
        }
        return PdfFontFactory.createFont(fallbackFont);
    }

    private Path resolveOutputPath(DataModelForZalik data) {
        String fileName = Objects.toString(data.groupName(), "group") + "_"
                + outputSuffix(data) + "_"
                + Objects.toString(data.semesterNumber(), "0") + "_"
                + Objects.toString(data.order(), "00") + ".pdf";
        return Paths.get("uploads").resolve(fileName);
    }

    private int parseToInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String convertMarkToNationalGrade(int mark) {
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

    private String convertMarkToECTSGrade(int mark) {
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

    private String formatDate(DataModelForZalik data) {
        String day = Objects.toString(data.day(), "").trim();
        String month = Objects.toString(data.month(), "").trim();
        String year = Objects.toString(data.year(), "").trim();
        if (!day.isEmpty() && !month.isEmpty() && !year.isEmpty()) {
            return String.format("%s.%s.%s", day, month, year);
        }
        return "";
    }
}
