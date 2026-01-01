package com.esvar.dekanat.generate.pdf;

import com.esvar.dekanat.document.DocumentException;
import com.esvar.dekanat.document.PdfGenerator;
import com.esvar.dekanat.generate.DataModelForZalik;
import com.esvar.dekanat.generate.StudentModelToDocumentGenerate;
import com.esvar.dekanat.generate.dto.ZalikRowDto;
import com.esvar.dekanat.generate.dto.ZalikSheetDto;
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
import com.itextpdf.layout.properties.Overflow;
import com.itextpdf.layout.properties.Property;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * iText 7 generator for "ВІДОМІСТЬ ОБЛІКУ УСПІШНОСТІ" (залік/екзамен) sheets.
 *
 * <p>Usage: {@code byte[] pdfBytes = ZalikSheetPdfGenerator.generate(dto);}.</p>
 */
@Component
public final class ZalikSheetPdfGenerator implements PdfGenerator {

    public static final String NAME = "zalik-sheet";

    private static final float[] STUDENT_COLUMN_WIDTHS = new float[]{5, 26, 14, 13, 13, 7, 12, 10};
    private static final float[] SUMMARY_COLUMN_WIDTHS = new float[]{18, 18, 12, 26, 26};
    private static final float HEADER_FONT_SIZE = 12f;
    private static final float BODY_FONT_SIZE = 10f;
    private static final float SMALL_FONT_SIZE = 8f;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Path generatePdf(Object data) {
        ZalikSheetDto dto = mapToDto(data);
        try {
            byte[] pdfBytes = generate(dto);
            Path outputPath = resolveOutputPath(dto);
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, pdfBytes);
            return outputPath;
        } catch (IOException ex) {
            throw new DocumentException("Failed to generate zalik sheet PDF", ex);
        }
    }

    /**
     * Generate the zalik sheet PDF as a byte array.
     */
    public static byte[] generate(ZalikSheetDto dto) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (PdfWriter writer = new PdfWriter(outputStream);
                 PdfDocument pdfDocument = new PdfDocument(writer);
                 Document document = new Document(pdfDocument, PageSize.A4)) {

                PdfFont regular = loadFont("/fonts/times.ttf", StandardFonts.TIMES_ROMAN);
                PdfFont bold = loadFont("/fonts/timesbd.ttf", StandardFonts.TIMES_BOLD);

                document.setFont(regular);
                document.setMargins(36, 36, 36, 36);

                buildHeader(document, dto, regular, bold);
                document.add(buildStudentsTable(dto, regular, bold));
                buildDeanBlock(document, dto, regular);
                document.add(new Paragraph("Підсумки складання екзамену (заліку)")
                        .setFont(bold)
                        .setFontSize(HEADER_FONT_SIZE)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(12f));
                document.add(buildSummaryTable(dto, regular, bold));
                buildExaminerBlock(document, dto, regular, bold);
            }
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new DocumentException("Failed to generate zalik sheet PDF", ex);
        }
    }

    private static void buildHeader(Document document, ZalikSheetDto dto, PdfFont regular, PdfFont bold) {
        document.add(new Paragraph("НАЦІОНАЛЬНИЙ ТРАНСПОРТНИЙ УНІВЕРСИТЕТ")
                .setFont(bold)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER));

        SolidLine solidLine = new SolidLine(1f);
        LineSeparator line = new LineSeparator(solidLine);
        line.setMarginTop(-6);
        line.setMarginBottom(0);

        document.add(line);
        document.add(new Paragraph("Спеціальність " + safe(dto.spec()))
                .setFont(regular)
                .setFontSize(11));
        document.add(line);



        Table groupTable = new Table(UnitValue.createPercentArray(new float[]{25, 10, 25, 40}))
                .useAllAvailableWidth();

        groupTable.addCell(new Cell().setPadding(0)
                .add(new Paragraph("Курс: ").setFont(regular).setFontSize(11))
                .setBorder(Border.NO_BORDER));
        groupTable.addCell(new Cell().setPadding(0)
                .add(new Paragraph(Objects.toString(dto.courseNumber(), ""))
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
                .add(new Paragraph(Objects.toString(dto.groupName(), ""))
                        .setFont(regular)
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(0.5f)));

        document.add(groupTable);

        document.add(new Paragraph(Objects.toString(dto.studyYear(), "") + " навчальний рік")
                .setFont(regular)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("ВІДОМІСТЬ ОБЛІКУ УСПІШНОСТІ № " + safe(dto.sheetNumber()))
                .setFont(bold)
                .setFontSize(HEADER_FONT_SIZE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(6f));

        String headerDate = buildHeaderDate(dto);
        if (!headerDate.isBlank()) {
            document.add(new Paragraph(headerDate)
                    .setFont(bold)
                    .setFontSize(BODY_FONT_SIZE)
                    .setTextAlignment(TextAlignment.CENTER));
        }

        document.add(new Paragraph("з " + safe(dto.disciplineName()))
                .setFont(regular)
                .setFontSize(BODY_FONT_SIZE)
                .setTextAlignment(TextAlignment.LEFT));
        document.add(new Paragraph("(назва дисципліни)")
                .setFont(regular)
                .setFontSize(SMALL_FONT_SIZE)
                .setTextAlignment(TextAlignment.LEFT));

        document.add(new Paragraph("за " + dto.semestrNumber() + "-й навчальний семестр.")
                .setFont(regular)
                .setFontSize(BODY_FONT_SIZE)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginTop(4f));

        Table controlInfo = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .useAllAvailableWidth();
        controlInfo.addCell(noBorderCell("Форма семестрового контролю: " + safe(dto.controlTypeName()), regular, TextAlignment.LEFT));
        controlInfo.addCell(noBorderCell("Загальна кількість годин " + dto.hours(), regular, TextAlignment.RIGHT));
        document.add(controlInfo);

        Table teachersTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth();
        teachersTable.addCell(teacherBlock("Викладач", safe(dto.teacherFullName1()), regular,
                "( прізвище, ім’я та по батькові викладача, який виставляє підсумкову оцінку)"));
        teachersTable.addCell(teacherBlock("Викладач", safe(dto.teacherFullName2()), regular,
                "( прізвище, ім’я та по батькові викладача, який здійснював поточний контроль)"));
        document.add(teachersTable);

        document.add(new Paragraph("").setMarginBottom(8f));
    }

    private static Table buildStudentsTable(ZalikSheetDto dto, PdfFont regular, PdfFont bold) {
        Table table = new Table(UnitValue.createPercentArray(STUDENT_COLUMN_WIDTHS))
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

        List<ZalikRowDto> rows = dto.data();
        if (rows.isEmpty()) {
            // Ensure layout stability even with no data rows.
            for (int col = 0; col < 8; col++) {
                table.addCell(bodyCell("", regular, TextAlignment.CENTER));
            }
            return table;
        }

        for (ZalikRowDto row : rows) {
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

    private static void buildDeanBlock(Document document, ZalikSheetDto dto, PdfFont regular) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{28, 24, 48}))
                .useAllAvailableWidth();
        table.setMarginTop(12f);

        table.addCell(noBorderCell("Декан факультету", regular, TextAlignment.LEFT));
        table.addCell(signatureLine("", regular));
        table.addCell(signatureLine(safe(dto.headName()), regular));

        table.addCell(noBorderCell("", regular, TextAlignment.LEFT));
        table.addCell(hintCell("(підпис)", regular));
        table.addCell(hintCell("(прізвище,ініціали)", regular));

        document.add(table);
    }

    private static Table buildSummaryTable(ZalikSheetDto dto, PdfFont regular, PdfFont bold) {
        Table table = new Table(UnitValue.createPercentArray(SUMMARY_COLUMN_WIDTHS))
                .useAllAvailableWidth();
        table.setMarginTop(12f);

        table.addCell(headerCell("ВСЬОГО ОЦІНОК", bold, 2, 1));
        table.addCell(headerCell("СУМА БАЛІВ", bold, 2, 1));
        table.addCell(headerCell("ОЦІНКА   ECTS", bold, 2, 1));
        table.addCell(headerCell("ОЦІНКА ЗА НАЦІОНАЛЬНОЮ ШКАЛОЮ", bold, 1, 2));

        table.addCell(headerCell("екзамен", bold));
        table.addCell(headerCell("залік", bold));

        addSummaryRow(table, bold, regular, dto.countA(), "90-100", "A", "відмінно", "зараховано");
        addSummaryRow(table, bold, regular, dto.countB(), "82-89", "B", "добре", "");
        addSummaryRow(table, bold, regular, dto.countC(), "74-81", "C", "", "");
        addSummaryRow(table, bold, regular, dto.countD(), "64-73", "D", "задовільно", "");
        addSummaryRow(table, bold, regular, dto.countE(), "60-63", "E", "", "");
        addSummaryRow(table, bold, regular, dto.countFx(), "35-59", "FX", "незадовільно", "незараховано");
        addSummaryRow(table, bold, regular, dto.countF(), "1-34", "F", "", "");

        return table;
    }

    private static void buildExaminerBlock(Document document, ZalikSheetDto dto, PdfFont regular, PdfFont bold) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 25, 45}))
                .useAllAvailableWidth();
        table.setMarginTop(12f);

        table.addCell(noBorderCell("Екзаменатор (викладач)", bold, TextAlignment.LEFT));
        table.addCell(signatureLine("", regular));
        table.addCell(signatureLine(safe(dto.teacherInitials()), regular));

        table.addCell(noBorderCell("", regular, TextAlignment.LEFT));
        table.addCell(hintCell("(підпис)", regular));
        table.addCell(hintCell("(прізвище,ініціали)", regular));

        document.add(table);
    }

    private static void addSummaryRow(Table table, PdfFont bold, PdfFont regular, int count,
                                      String points, String ects, String examMark, String zalikMark) {
        table.addCell(bodyCell(String.valueOf(count), bold, TextAlignment.CENTER));
        table.addCell(bodyCell(points, regular, TextAlignment.CENTER));
        table.addCell(bodyCell(ects, regular, TextAlignment.CENTER));
        table.addCell(bodyCell(examMark, regular, TextAlignment.CENTER));
        table.addCell(bodyCell(zalikMark, regular, TextAlignment.CENTER));
    }

    private static Cell headerCell(String text, PdfFont font) {
        return headerCell(text, font, 1, 1);
    }

    private static Cell headerCell(String text, PdfFont font, int rowSpan, int colSpan) {
        return new Cell(rowSpan, colSpan)
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(BODY_FONT_SIZE)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(new SolidBorder(0.5f))
                .setPadding(4f);
    }

    private static Cell numberHeaderCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(BODY_FONT_SIZE)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(new SolidBorder(0.5f))
                .setPadding(3f);
    }

    private static Cell bodyCell(String text, PdfFont font, TextAlignment alignment) {
        Paragraph paragraph = new Paragraph(safe(text))
                .setFont(font)
                .setFontSize(BODY_FONT_SIZE)
                .setTextAlignment(alignment)
                .setProperty(Property.NO_WRAP, true)
                .setProperty(Property.OVERFLOW, Overflow.HIDDEN);

        return new Cell()
                .add(paragraph)
                .setBorder(new SolidBorder(0.5f))
                .setPadding(4f);
    }

    private static Cell noBorderCell(String text, PdfFont font, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(safe(text))
                        .setFont(font)
                        .setFontSize(BODY_FONT_SIZE)
                        .setTextAlignment(alignment))
                .setBorder(Border.NO_BORDER)
                .setPadding(2f);
    }

    private static Cell hintCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(SMALL_FONT_SIZE)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER)
                .setPadding(0f);
    }

    private static Cell signatureLine(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(safe(text))
                        .setFont(font)
                        .setFontSize(BODY_FONT_SIZE)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(0.5f))
                .setPadding(2f);
    }

    private static Cell teacherBlock(String label, String value, PdfFont font, String hint) {
        Cell cell = new Cell().setBorder(Border.NO_BORDER);
        cell.add(new Paragraph(label)
                .setFont(font)
                .setFontSize(BODY_FONT_SIZE)
                .setTextAlignment(TextAlignment.CENTER));
        cell.add(new Paragraph(value)
                .setFont(font)
                .setFontSize(BODY_FONT_SIZE)
                .setTextAlignment(TextAlignment.CENTER));
        cell.add(new Paragraph(hint)
                .setFont(font)
                .setFontSize(SMALL_FONT_SIZE)
                .setTextAlignment(TextAlignment.CENTER));
        return cell;
    }

    private static String buildHeaderDate(ZalikSheetDto dto) {
        LocalDate date = dto.sheetDate();
        String day = safe(dto.sheetDay());
        String month = safe(dto.sheetMonth());
        String year = safe(dto.sheetYear());

        if (date != null) {
            day = String.format("%02d", date.getDayOfMonth());
            month = date.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("uk"));
            year = String.valueOf(date.getYear());
        }

        if (day.isBlank() && month.isBlank() && year.isBlank()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        if (!day.isBlank()) {
            builder.append("\"").append(day).append("\" ");
        }
        if (!month.isBlank()) {
            builder.append(month).append(" ");
        }
        if (!year.isBlank()) {
            builder.append(year).append(" ");
        }
        builder.append("року");
        return builder.toString().trim();
    }

    private static String resolveRowDate(ZalikRowDto row) {
        if (row.date() != null) {
            return formatDate(row.date());
        }
        if (row.dateText() != null && !row.dateText().isBlank()) {
            return row.dateText();
        }
        return "____.__.____";
    }

    private static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    private static LocalDate parseSheetDate(String day, String month, String year) {
        try {
            if (day == null || month == null || year == null) {
                return null;
            }
            int d = Integer.parseInt(day);
            int m = Integer.parseInt(month);
            int y = Integer.parseInt(year);
            return LocalDate.of(y, m, d);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * Compute study year in format "YY-(YY+1)" using current calendar year.
     */
    public static String computeStudyYear(LocalDate now) {
        int year = Objects.requireNonNullElse(now, LocalDate.now()).getYear() % 100;
        int nextYear = (year + 1) % 100;
        return String.format("%02d-%02d", year, nextYear);
    }

    private ZalikSheetDto mapToDto(Object data) {
        if (data instanceof ZalikSheetDto dto) {
            return dto;
        }
        if (data instanceof DataModelForZalik zalik) {
            return fromDataModel(zalik);
        }
        throw new DocumentException("Expected ZalikSheetDto or DataModelForZalik");
    }

    private ZalikSheetDto fromDataModel(DataModelForZalik data) {
        List<ZalikRowDto> rows = data.students().stream()
                .map(student -> toRow(student, data))
                .toList();

        String studyYear = safe(data.studyYear());
        if (studyYear.isBlank()) {
            studyYear = computeStudyYear(LocalDate.now());
        }

        return new ZalikSheetDto(
                safe(data.specialityName()),
                safe(data.courseNumber()),
                safe(data.groupName()),
                studyYear,
                safe(data.order()),
                parseSheetDate(data.day(), data.month(), data.year()),
                safe(data.day()),
                safe(data.month()),
                safe(data.year()),
                safe(data.disciplineName()),
                parseToInt(data.semesterNumber()),
                safe(data.controlTypeName()),
                parseToInt(data.hours()),
                safe(data.firstTeacher()),
                safe(data.secondTeacher()),
                safe(data.dean()),
                safe(data.gradeTeacher()),
                rows,
                parseToInt(data.a()),
                parseToInt(data.b()),
                parseToInt(data.c()),
                parseToInt(data.d()),
                parseToInt(data.e()),
                parseToInt(data.fx()),
                parseToInt(data.f())
        );
    }

    private ZalikRowDto toRow(StudentModelToDocumentGenerate student, DataModelForZalik data) {
        int markValue = parseToInt(student.mark());
        return new ZalikRowDto(
                student.index(),
                safe(student.name()),
                safe(student.studentNumber()),
                convertMarkToNationalGrade(markValue),
                safe(student.mark()),
                convertMarkToECTSGrade(markValue),
                null,
                formatRowDate(data),
                ""
        );
    }

    private String formatRowDate(DataModelForZalik data) {
        String day = safe(data.day());
        String month = safe(data.month());
        String year = safe(data.year());
        if (day.isBlank() || month.isBlank() || year.isBlank()) {
            return "";
        }
        return String.format("%s.%s.%s", day, month, year);
    }

    private Path resolveOutputPath(ZalikSheetDto dto) {
        String sheetNumber = safe(dto.sheetNumber());
        if (sheetNumber.isBlank()) {
            sheetNumber = "zalik";
        }
        String fileName = "zalik_sheet_" + sheetNumber + ".pdf";
        return Paths.get("uploads").resolve(fileName);
    }

    private int parseToInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
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

    private static PdfFont loadFont(String resourcePath, String fallbackFont) throws IOException {
        try (InputStream fontStream = ZalikSheetPdfGenerator.class.getResourceAsStream(resourcePath)) {
            if (fontStream != null) {
                FontProgram fontProgram = FontProgramFactory.createFont(fontStream.readAllBytes());
                return PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H);
            }
        }
        return PdfFontFactory.createFont(fallbackFont);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
