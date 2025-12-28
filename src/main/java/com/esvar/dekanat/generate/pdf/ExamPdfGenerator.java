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
import com.itextpdf.layout.element.*;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * PDF generator for exam control statements.
 */
@Component
public class ExamPdfGenerator implements PdfGenerator {

    public static final String NAME = "exam";

    private static final Logger log = LoggerFactory.getLogger(BaseZalikStylePdfGenerator.class);

    @Override
    public String getName() {
        return "";
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
                addStudentsAndSummarySections(document, regular, bold, zalikData);

                log.info("Generated {} PDF at {}", getName(), outputPath);
            }

            return outputPath;
        } catch (IOException e) {
            throw new DocumentException("Failed to generate PDF", e);
        }
    }

    private void addStudentsAndSummarySections(Document document, PdfFont regular, PdfFont bold,
                                               DataModelForZalik zalikData) {
        List<StudentModelToDocumentGenerate> students =
                zalikData.students() == null ? Collections.emptyList() : zalikData.students();

        if (students.size() > 1) {
            List<StudentModelToDocumentGenerate> mainRows = students.subList(0, students.size() - 1);
            document.add(buildStudentsTable(mainRows, regular, bold));
        }

        List<StudentModelToDocumentGenerate> lastRows = students.isEmpty()
                ? Collections.emptyList()
                : List.of(students.get(students.size() - 1));

        Div tailSection = new Div().setKeepTogether(true);
        tailSection.add(buildStudentsTable(lastRows, regular, bold));
        tailSection.add(buildDeanBlock(zalikData, regular));
        tailSection.add(buildSummaryTable(zalikData, regular, bold));

        tailSection.add(new Paragraph("\s"));
        tailSection.add(buildSignatureSection(zalikData, regular, bold));

        document.add(tailSection);


    }



    private Table buildSignatureSection(DataModelForZalik data, PdfFont regular, PdfFont bold) {
        Table signatureTable = new Table(UnitValue.createPercentArray(new float[]{20, 5, 20, 5, 20}))
                .useAllAvailableWidth();
        signatureTable.setKeepTogether(true);

        signatureTable.addCell(createSignatureLabelCell(bold));
        signatureTable.addCell(createSignatureSpacerCell());
        signatureTable.addCell(createSignatureLineCell(bold));
        signatureTable.addCell(createSignatureSpacerCell());
        signatureTable.addCell(createSignatureNameCell(data.gradeTeacher(), bold));

        signatureTable.addCell(createSignatureSpacerCell());
        signatureTable.addCell(createSignatureSpacerCell());
        signatureTable.addCell(createSignatureHintCell("(підпис)", regular));
        signatureTable.addCell(createSignatureSpacerCell());
        signatureTable.addCell(createSignatureHintCell("(прізвище,ініціали)", regular));

        return signatureTable;
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
                .add(new Paragraph("\s"));
    }

    protected String outputSuffix(DataModelForZalik data) {
        return NAME; // або "exam"
    }


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
        document.add(new Paragraph(data.facultyName() == null || data.facultyName().isBlank() ? " " : data.facultyName())
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
                .add(new Paragraph("Курс: ")
                        .setFont(regular)
                        .setFontSize(11))
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
                .add(new Paragraph("\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0Група: ")
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
        document.add(new Paragraph("ВІДОМІСТЬ ОБЛІКУ УСПІШНОСТІ № " + Objects.toString(data.order(), ""))
                .setFont(bold)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph(String.format("%s  %s  %s року", data.day(), data.month(), data.year()))
                .setFont(bold)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER)
                .setUnderline());

        document.add(createDisciplineTable(regular, data));
        document.add(createSemesterTable(regular, data));
        document.add(createSemesterControlTable(regular, data));

        document.add(createTeacherTable("Викладач", Objects.toString(data.firstTeacher(), ""), regular));
        document.add(createTeacherTable("Викладач", Objects.toString(data.secondTeacher(), ""), regular,
                "(прізвище, ім’я та по батькові викладача, який здійснював поточний контроль)"));

        document.add(new Paragraph("\s")
                .setFont(regular)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER));
    }

    private Table createDisciplineTable(PdfFont regular, DataModelForZalik data) {
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
        return disciplineTable;
    }

    private Table createSemesterTable(PdfFont regular, DataModelForZalik data) {
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
        return semControlTable;
    }

    private Table createSemesterControlTable(PdfFont regular, DataModelForZalik data) {
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
        return semesterControlTable;
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

    private static Table buildStudentsTable(List<StudentModelToDocumentGenerate> rows, PdfFont regular,
                                            PdfFont bold) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{5, 26, 14, 13, 13, 7, 12, 10}))
                .useAllAvailableWidth();



        table.addHeaderCell(headerCell("№ з/п", bold, 2, 1));
        table.addHeaderCell(headerCell("Прізвище та ініціали студента", bold, 2, 1));
        table.addHeaderCell(headerCell("№ залікової книжки", bold, 2, 1));
        table.addHeaderCell(headerCell("Оцінка", bold, 1, 3));
        table.addHeaderCell(headerCell("Дата", bold, 2, 1));
        table.addHeaderCell(headerCell("Підпис викладача", bold, 2, 1));

        table.addHeaderCell(headerCell("за національною шкалою", bold));
        table.addHeaderCell(headerCell("кількість балів за 100 бальною шкалою", bold));
        table.addHeaderCell(headerCell("ECTS", bold));

        for (int i = 1; i <= 8; i++) {
            table.addHeaderCell(numberHeaderCell(String.valueOf(i), regular));
        }

        if (rows.isEmpty()) {
            // Ensure layout stability even with no data rows.
            for (int col = 0; col < 8; col++) {
                table.addCell(bodyCell("", regular, TextAlignment.CENTER));
            }
            return table;
        }

        for (StudentModelToDocumentGenerate row : rows) {
            table.addCell(bodyCell(row.index() + ".", regular, TextAlignment.CENTER));
            table.addCell(bodyCell(safe(row.name()), regular, TextAlignment.LEFT));
            table.addCell(bodyCell(safe(row.studentNumber()), regular, TextAlignment.CENTER));
            table.addCell(bodyCell(safe(row.nationalMark()), regular, TextAlignment.CENTER));
            table.addCell(bodyCell(safe(row.mark()), regular, TextAlignment.CENTER));
            table.addCell(bodyCell(safe(row.ectsMark()), regular, TextAlignment.CENTER));
            table.addCell(bodyCell(resolveRowDate(row), regular, TextAlignment.CENTER));
            table.addCell(bodyCell(safe(row.teacherSignPlaceholder()), regular, TextAlignment.CENTER));
        }


        return table;
    }



    private static String resolveRowDate(StudentModelToDocumentGenerate row) {
        if (row.date() != null) {
            return formatDate(row.date()); // Виклик методу, що приймає LocalDate
        }
        if (row.dateText() != null && !row.dateText().isBlank()) {
            return row.dateText();
        }
        return "";
    }

    private static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    private static Cell numberHeaderCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(10f)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(new SolidBorder(0.5f))
                .setPadding(3f);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static Cell bodyCell(String text, PdfFont font, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(safe(text))
                        .setFont(font)
                        .setFontSize(10f)
                        .setTextAlignment(alignment))
                .setBorder(new SolidBorder(0.5f))
                .setPadding(4f);
    }

    private static Cell headerCell(String text, PdfFont font) {
        return headerCell(text, font, 1, 1);
    }

    private static Cell headerCell(String text, PdfFont font, int rowSpan, int colSpan) {
        return new Cell(rowSpan, colSpan)
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(10f)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(new SolidBorder(0.5f))
                .setPadding(4f);
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

    private static Table buildSummaryTable(DataModelForZalik dto, PdfFont regular, PdfFont bold) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{18, 18, 12, 52}))
                .useAllAvailableWidth();
        table.setMarginTop(12f);

        // Header row 1
        table.addCell(headerCell("ВСЬОГО ОЦІНОК", bold, 2, 1));
        table.addCell(headerCell("СУМА БАЛІВ", bold, 2, 1));
        table.addCell(headerCell("ОЦІНКА\nECTS", bold, 2, 1));
        table.addCell(headerCell("ОЦІНКА ЗА НАЦІОНАЛЬНОЮ ШКАЛОЮ", bold, 1, 1));

        // Header row 2 (only for last column)
        table.addCell(headerCell("залік", bold));

        // A row (put rowspan cell here)
        addRow3(table, regular, Integer.parseInt(dto.a()), "90-100", "A");
        Cell passed = bodyCell("Зараховано", regular, TextAlignment.CENTER, 5, 1);
        passed.setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
        passed.setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
        table.addCell(passed);

        // B–E rows (ONLY 3 cells each!)
        addRow3(table, regular, Integer.parseInt(dto.b()), "82-89", "B");
        addRow3(table, regular, Integer.parseInt(dto.c()), "74-81", "C");
        addRow3(table, regular, Integer.parseInt(dto.d()), "64-73", "D");
        addRow3(table, regular, Integer.parseInt(dto.e()), "60-63", "E");

        // FX row (put rowspan cell here)
        addRow3(table, regular, Integer.parseInt(dto.fx()), "35-59", "FX");
        Cell failed = bodyCell("Незараховано", regular, TextAlignment.CENTER, 2 , 1);
        failed.setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
        failed.setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
        table.addCell(failed);

        // F row (ONLY 3 cells)
        addRow3(table, regular, Integer.parseInt(dto.f()), "1-34", "F");

        return table;
    }

    private static void addRow3(Table table, PdfFont font, int count, String points, String ects) {
        table.addCell(bodyCell(String.valueOf(count), font, TextAlignment.CENTER));
        table.addCell(bodyCell(points, font, TextAlignment.CENTER));
        table.addCell(bodyCell(ects, font, TextAlignment.CENTER));
    }


    private static void addRowWithoutZalik(
            Table table,
            PdfFont font,
            int count,
            String points,
            String ects
    ) {
        table.addCell(bodyCell(String.valueOf(count), font, TextAlignment.CENTER));
        table.addCell(bodyCell(points, font, TextAlignment.CENTER));
        table.addCell(bodyCell(ects, font, TextAlignment.CENTER));
    }


    private static void addSummaryRowZalik(Table table,
                                           PdfFont bold,
                                           PdfFont regular,
                                           int totalGrades,
                                           String pointsRange,
                                           String ects,
                                           String zalikText) {
        // 1) ВСЬОГО ОЦІНОК
        table.addCell(bodyCell(String.valueOf(totalGrades), regular, TextAlignment.CENTER));

        // 2) СУМА БАЛІВ
        table.addCell(bodyCell(pointsRange, regular, TextAlignment.CENTER));

        // 3) ECTS
        table.addCell(bodyCell(ects, regular, TextAlignment.CENTER));

        // 4-5) Національна (залік) — створюємо Cell з colspan=2 через конструктор new Cell(rowspan, colspan)
        table.addCell(bodyCell(zalikText, regular, TextAlignment.CENTER, 1, 2));
    }

    private static Cell bodyCell(String text, PdfFont font, TextAlignment alignment, int rowSpan, int colSpan) {
        return new Cell(rowSpan, colSpan)
                .add(new Paragraph(safe(text))
                        .setFont(font)
                        .setFontSize(10f)
                        .setTextAlignment(alignment))
                .setBorder(new SolidBorder(0.5f))
                .setPadding(4f);
    }


    private static void addSummaryRow(Table table, PdfFont bold, PdfFont regular, int count,
                                      String points, String ects, String examMark, String zalikMark) {
        table.addCell(bodyCell(String.valueOf(count), bold, TextAlignment.CENTER));
        table.addCell(bodyCell(points, regular, TextAlignment.CENTER));
        table.addCell(bodyCell(ects, regular, TextAlignment.CENTER));
        table.addCell(bodyCell(examMark, regular, TextAlignment.CENTER));
        table.addCell(bodyCell(zalikMark, regular, TextAlignment.CENTER));
    }

    private static Table buildDeanBlock(DataModelForZalik dto, PdfFont regular) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{28, 24, 48}))
                .useAllAvailableWidth();
        table.setMarginTop(12f);

        table.addCell(noBorderCell("Декан факультету", regular, TextAlignment.LEFT));
        table.addCell(signatureLine("", regular));
        table.addCell(signatureLine(safe(dto.dean()), regular));

        table.addCell(noBorderCell("", regular, TextAlignment.LEFT));
        table.addCell(hintCell("(підпис)", regular));
        table.addCell(hintCell("(прізвище,ініціали)", regular));

        return table;
    }

    private static Cell noBorderCell(String text, PdfFont font, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(safe(text))
                        .setFont(font)
                        .setFontSize(10f)
                        .setTextAlignment(alignment))
                .setBorder(Border.NO_BORDER)
                .setPadding(2f);
    }

    private static Cell hintCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(8f)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER)
                .setPadding(0f);
    }

    private static Cell signatureLine(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(safe(text))
                        .setFont(font)
                        .setFontSize(10f)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(0.5f))
                .setPadding(2f);
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
