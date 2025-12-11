package com.esvar.dekanat.generate.pdf;

import com.esvar.dekanat.document.DocumentException;
import com.esvar.dekanat.document.PdfGenerator;
import com.esvar.dekanat.generate.DataModelForZalik;
import com.esvar.dekanat.generate.StudentModelToDocumentGenerate;
import com.esvar.dekanat.generate.ZalikGenerator;
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
import com.itextpdf.layout.properties.VerticalAlignment;
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
 * PDF generator for "Відомість обліку успішності" (залік).
 */
@Component
public class ZalikPdfGenerator implements PdfGenerator {

    public static final String NAME = ZalikGenerator.NAME;

    private static final Logger log = LoggerFactory.getLogger(ZalikPdfGenerator.class);

    @Override
    public String getName() {
        return NAME;
    }

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
                addFooter(document, regular, bold, zalikData);

                log.info("Generated zalik PDF at {}", outputPath);
            }

            return outputPath;
        } catch (IOException e) {
            throw new DocumentException("Failed to generate zalik PDF", e);
        }
    }

    private void addHeader(Document document, PdfFont regular, PdfFont bold, DataModelForZalik data) {
        document.setFontSize(11);

        document.add(new Paragraph("НАЦІОНАЛЬНИЙ ТРАНСПОРТНИЙ УНІВЕРСИТЕТ")
                .setFont(bold)
                .setTextAlignment(TextAlignment.CENTER));

        SolidLine solidLine = new SolidLine(1f);
        LineSeparator separator = new LineSeparator(solidLine);
        separator.setMarginTop(-6);
        separator.setMarginBottom(2);

        document.add(separator);
        document.add(new Paragraph("Факультет " + Objects.toString(data.facultyName(), ""))
                .setFont(regular)
                .setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("за " + Objects.toString(data.studyYear(), "") + " навчальний рік")
                .setFont(regular)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("ВІДОМІСТЬ ОБЛІКУ УСПІШНОСТІ № " + Objects.toString(data.order(), ""))
                .setFont(bold)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(4)
                .setMarginBottom(6));

        Table courseTable = new Table(UnitValue.createPercentArray(new float[]{22, 14, 24, 14, 26}))
                .useAllAvailableWidth();
        courseTable.addCell(labelCell("для курсу", regular));
        courseTable.addCell(underlinedValueCell(Objects.toString(data.courseNumber(), ""), regular));
        courseTable.addCell(labelCell("Група", regular));
        courseTable.addCell(underlinedValueCell(Objects.toString(data.groupName(), ""), regular));
        courseTable.addCell(labelCell("за " + Objects.toString(data.semesterNumber(), "") + " семестр", regular));

        document.add(courseTable);

        document.add(new Paragraph("Спеціальність: " + Objects.toString(data.specialityName(), ""))
                .setFont(regular)
                .setMarginTop(4));

        Table disciplineTable = new Table(UnitValue.createPercentArray(new float[]{20, 50, 30}))
                .useAllAvailableWidth();
        disciplineTable.addCell(labelCell("Дисципліна: ", regular));
        disciplineTable.addCell(underlinedValueCell(Objects.toString(data.disciplineName(), ""), regular));
        disciplineTable.addCell(labelCell("годин (навчальний план)", regular));
        document.add(disciplineTable);

        Table controlTable = new Table(UnitValue.createPercentArray(new float[]{28, 32, 18, 22}))
                .useAllAvailableWidth();
        controlTable.addCell(labelCell("Форма семестрового контролю:", regular));
        controlTable.addCell(underlinedValueCell(Objects.toString(data.controlTypeName(), ""), regular));
        controlTable.addCell(labelCell("Загальна кількість годин:", regular));
        controlTable.addCell(underlinedValueCell(Objects.toString(data.hours(), ""), regular));
        document.add(controlTable);

        Table teachersTable = new Table(UnitValue.createPercentArray(new float[]{33, 33, 34}))
                .useAllAvailableWidth();
        teachersTable.addCell(labelWithValue("Викладач(ка):", Objects.toString(data.firstTeacher(), ""), regular));
        teachersTable.addCell(labelWithValue("Викладач лабораторних занять:", Objects.toString(data.secondTeacher(), ""), regular));
        teachersTable.addCell(labelWithValue("Староста:", "", regular));
        document.add(teachersTable);

        document.add(new Paragraph("Декан факультету: " + Objects.toString(data.dean(), ""))
                .setFont(regular)
                .setMarginTop(2)
                .setMarginBottom(8));
    }

    private void addStudentsTable(Document document, PdfFont regular, PdfFont bold, DataModelForZalik data) {
        document.add(new Paragraph("Підсумки оцінювання семестру (оцінки)")
                .setFont(bold)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(4));

        float[] columnWidths = {5, 30, 16, 12, 18, 12, 7};
        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .useAllAvailableWidth();

        addHeaderCell(table, "№ з/п", bold);
        addHeaderCell(table, "Прізвище, ім'я та по батькові", bold);
        addHeaderCell(table, "Номер залікової книжки", bold);
        addHeaderCell(table, "Бали (0-100)", bold);
        addHeaderCell(table, "Оцінка за національною шкалою", bold);
        addHeaderCell(table, "Оцінка за шкалою ECTS", bold);
        addHeaderCell(table, "Підпис", bold);

        List<StudentModelToDocumentGenerate> students = data.students();
        for (StudentModelToDocumentGenerate student : students) {
            int numericMark = parseToInt(student.mark());
            String nationalGrade = convertMarkToNationalGrade(numericMark);
            String ectsGrade = convertMarkToECTSGrade(numericMark);

            table.addCell(defaultCell(String.valueOf(student.index()), regular, TextAlignment.CENTER));
            table.addCell(defaultCell(student.name(), regular, TextAlignment.LEFT));
            table.addCell(defaultCell(student.studentNumber(), regular, TextAlignment.CENTER));
            table.addCell(defaultCell(student.mark(), regular, TextAlignment.CENTER));
            table.addCell(defaultCell(nationalGrade, regular, TextAlignment.CENTER));
            table.addCell(defaultCell(ectsGrade, regular, TextAlignment.CENTER));
            table.addCell(defaultCell("", regular, TextAlignment.CENTER));
        }

        document.add(table);
    }

    private void addFooter(Document document, PdfFont regular, PdfFont bold, DataModelForZalik data) {
        int totalMarks = calculateTotalMarks(data.students());
        int studentCount = data.students() == null ? 0 : data.students().size();
        int averageMark = studentCount > 0 ? Math.round((float) totalMarks / studentCount) : 0;

        document.add(new Paragraph().setMarginTop(6));

        Table resultTable = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                .useAllAvailableWidth();
        resultTable.addCell(summaryCell("ВСЬОГО ОЦІНОК (бали)", "100", regular, bold));
        resultTable.addCell(summaryCell("СУМА ОЦІНОК (бали)", String.valueOf(totalMarks), regular, bold));
        resultTable.addCell(summaryCell("ОЦІНКА ЗА НАЦІОНАЛЬНОЮ ШКАЛОЮ", convertMarkToNationalGrade(averageMark), regular, bold));
        resultTable.addCell(summaryCell("ОЦІНКА за шкалою ECTS", convertMarkToECTSGrade(averageMark), regular, bold));
        document.add(resultTable);

        document.add(new Paragraph().setMarginTop(4));

        Table distribution = new Table(UnitValue.createPercentArray(new float[]{12, 12, 12, 12, 12, 12, 12}))
                .useAllAvailableWidth();
        addHeaderCell(distribution, "A", bold);
        addHeaderCell(distribution, "B", bold);
        addHeaderCell(distribution, "C", bold);
        addHeaderCell(distribution, "D", bold);
        addHeaderCell(distribution, "E", bold);
        addHeaderCell(distribution, "FX", bold);
        addHeaderCell(distribution, "F", bold);

        distribution.addCell(defaultCell(Objects.toString(data.a(), "0"), regular, TextAlignment.CENTER));
        distribution.addCell(defaultCell(Objects.toString(data.b(), "0"), regular, TextAlignment.CENTER));
        distribution.addCell(defaultCell(Objects.toString(data.c(), "0"), regular, TextAlignment.CENTER));
        distribution.addCell(defaultCell(Objects.toString(data.d(), "0"), regular, TextAlignment.CENTER));
        distribution.addCell(defaultCell(Objects.toString(data.e(), "0"), regular, TextAlignment.CENTER));
        distribution.addCell(defaultCell(Objects.toString(data.fx(), "0"), regular, TextAlignment.CENTER));
        distribution.addCell(defaultCell(Objects.toString(data.f(), "0"), regular, TextAlignment.CENTER));

        document.add(distribution);

        String date = formatDate(data);
        if (!date.isBlank()) {
            document.add(new Paragraph("Дата проведення: " + date)
                    .setFont(regular)
                    .setMarginTop(6));
        }

        Table signatures = new Table(UnitValue.createPercentArray(new float[]{34, 33, 33}))
                .useAllAvailableWidth();
        signatures.addCell(signatureCell("Екзаменатор", Objects.toString(data.gradeTeacher(), ""), regular));
        signatures.addCell(signatureCell("Декан", Objects.toString(data.dean(), ""), regular));
        signatures.addCell(signatureCell("Завідувач кафедри", Objects.toString(data.departmentName(), ""), regular));

        document.add(signatures);
    }

    private Cell labelCell(String text, PdfFont font) {
        return new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(11));
    }

    private Cell underlinedValueCell(String text, PdfFont font) {
        return new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBorderBottom(new SolidBorder(0.5f)));
    }

    private Cell labelWithValue(String label, String value, PdfFont font) {
        return new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(label)
                        .setFont(font)
                        .setFontSize(11))
                .add(new Paragraph(value)
                        .setFont(font)
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBorderBottom(new SolidBorder(0.5f)));
    }

    private void addHeaderCell(Table table, String text, PdfFont bold) {
        table.addHeaderCell(new Cell().add(new Paragraph(text)
                        .setFont(bold)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER))
                .setPadding(4)
                .setVerticalAlignment(VerticalAlignment.MIDDLE));
    }

    private Cell defaultCell(String text, PdfFont font, TextAlignment alignment) {
        return new Cell().add(new Paragraph(text == null ? "" : text)
                        .setFont(font)
                        .setFontSize(10)
                        .setTextAlignment(alignment))
                .setPadding(4)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell summaryCell(String label, String value, PdfFont font, PdfFont bold) {
        return new Cell().setPadding(6)
                .add(new Paragraph(label)
                        .setFont(font)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph(value)
                        .setFont(bold)
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER));
    }

    private Cell signatureCell(String label, String value, PdfFont font) {
        Paragraph labelParagraph = new Paragraph(label)
                .setFont(font)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER);
        Paragraph valueParagraph = new Paragraph(value)
                .setFont(font)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorderBottom(new SolidBorder(0.5f));

        return new Cell().setPadding(8)
                .add(labelParagraph)
                .add(valueParagraph)
                .setBorder(Border.NO_BORDER);
    }

    private int calculateTotalMarks(List<StudentModelToDocumentGenerate> students) {
        if (students == null || students.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (StudentModelToDocumentGenerate student : students) {
            total += parseToInt(student.mark());
        }
        return total;
    }

    private PdfFont loadFont(String resourcePath, String fallbackFont) throws IOException {
        try (InputStream fontStream = ZalikPdfGenerator.class.getResourceAsStream(resourcePath)) {
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
        String fileName = Objects.toString(data.groupName(), "group") + "_zalik_"
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
