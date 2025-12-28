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
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.layout.LayoutArea;
import com.itextpdf.layout.layout.LayoutContext;
import com.itextpdf.layout.layout.LayoutResult;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.renderer.DocumentRenderer;
import com.itextpdf.layout.renderer.IRenderer;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * PDF generator for course project statements with strict pagination rules.
 */
@Component
public class CourseProjectPdfGenerator implements PdfGenerator {

    public static final String NAME = "course-project";

    private static final Logger log = LoggerFactory.getLogger(CourseProjectPdfGenerator.class);
    private static final float BORDER_WIDTH = 0.5f;
    private static final float DEFAULT_FONT_SIZE = 11f;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Path generatePdf(Object data) {
        CourseProjectData projectData = CourseProjectData.from(data);

        try {
            Path outputPath = resolveOutputPath(projectData);
            Files.createDirectories(outputPath.getParent());

            try (PdfWriter writer = new PdfWriter(outputPath.toFile());
                 PdfDocument pdfDocument = new PdfDocument(writer);
                 Document document = new Document(pdfDocument, PageSize.A4)) {

                PdfFont regular = loadFont("/fonts/times.ttf", StandardFonts.TIMES_ROMAN);
                PdfFont bold = loadFont("/fonts/timesbd.ttf", StandardFonts.TIMES_BOLD);

                MeasuringDocumentRenderer renderer = new MeasuringDocumentRenderer(document);
                document.setRenderer(renderer);
                document.setFont(regular);

                addHeader(document, projectData, regular, bold);
                addStudentsWithFooter(document, projectData, regular, bold, renderer);

                log.info("Generated {} PDF at {}", getName(), outputPath);
            }

            return outputPath;
        } catch (IOException e) {
            throw new DocumentException("Failed to generate PDF", e);
        }
    }

    private void addHeader(Document document, CourseProjectData data, PdfFont regular, PdfFont bold) {
        document.add(centered("НАЦІОНАЛЬНИЙ ТРАНСПОРТНИЙ УНІВЕРСИТЕТ", bold, 11));

        SolidLine solidLine = new SolidLine(1f);
        document.add(createSeparator(solidLine));
        document.add(new Paragraph(safeText(data.facultyName()))
                .setFont(regular)
                .setFontSize(DEFAULT_FONT_SIZE));
        document.add(createSeparator(solidLine));
        document.add(new Paragraph("Спеціальність: " + safeText(data.specialityName()))
                .setFont(regular)
                .setFontSize(DEFAULT_FONT_SIZE));
        document.add(createSeparator(solidLine));

        document.add(buildCourseGroupRow(data, regular));
        document.add(centered(safeText(data.studyYear()) + " навчальний рік", regular, DEFAULT_FONT_SIZE));
        document.add(centered("ВІДОМІСТЬ ОБЛІКУ УСПІШНОСТІ № " + safeText(data.sheetNumber()), bold, DEFAULT_FONT_SIZE));
        document.add(centered(formatDate(data), bold, DEFAULT_FONT_SIZE).setUnderline());

        document.add(buildDisciplineRow(data, regular));
        document.add(buildSemesterRow(data, regular));
        document.add(buildTeacherRow("Викладач", data.teacherFullName1(),
                "( прізвище, ім’я та по батькові викладача, який виставляє підсумкову оцінку)", regular));
        document.add(buildTeacherRow("Викладач", data.teacherFullName2(),
                "( прізвище, ім’я та по батькові викладача, який здійснював поточний контроль)", regular));
        document.add(new Paragraph(" "));
    }

    private void addStudentsWithFooter(Document document,
                                       CourseProjectData data,
                                       PdfFont regular,
                                       PdfFont bold,
                                       MeasuringDocumentRenderer renderer) {
        List<StudentRow> students = data.students() == null ? Collections.emptyList() : data.students();

        if (students.size() > 1) {
            Table mainTable = buildStudentsTable(students.subList(0, students.size() - 1), regular, bold);
            document.add(mainTable);
        }

        List<StudentRow> lastRows = students.isEmpty()
                ? Collections.emptyList()
                : List.of(students.get(students.size() - 1));

        Div tailSection = new Div();
        tailSection.add(buildStudentsTable(lastRows, regular, bold));
        tailSection.add(buildFooter(data, regular, bold));

        float availableHeight = getAvailableHeight(document, renderer);
        float tailHeight = measureHeight(tailSection, document);

        if (tailHeight > availableHeight) {
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        }

        document.add(tailSection);
    }

    private Div buildFooter(CourseProjectData data, PdfFont regular, PdfFont bold) {
        Div footer = new Div();
        footer.add(buildDeanBlock(data, regular));
        if (!safeText(data.teacherFullName1()).isBlank()) {
            footer.add(new Paragraph(" "));
            footer.add(buildExaminerBlock(data.teacherFullName1(), regular, bold));
        }
        return footer;
    }

    private Table buildStudentsTable(List<StudentRow> rows, PdfFont regular, PdfFont bold) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{7, 33, 18, 20, 11, 11}))
                .useAllAvailableWidth();

        table.addHeaderCell(headerCell("№ з/п", bold));
        table.addHeaderCell(headerCell("Прізвище та ініціали студента", bold));
        table.addHeaderCell(headerCell("№ залікової книжки", bold));
        table.addHeaderCell(headerCell("Оцінка за курсовий проєкт", bold));
        table.addHeaderCell(headerCell("Дата", bold));
        table.addHeaderCell(headerCell("Підпис викладача", bold));

        for (int i = 1; i <= 6; i++) {
            table.addHeaderCell(numberHeaderCell(String.valueOf(i), regular));
        }

        if (rows.isEmpty()) {
            addEmptyRow(table, regular);
            return table;
        }

        for (StudentRow row : rows) {
            table.addCell(bodyCell(row.index() + ".", regular, TextAlignment.CENTER));
            table.addCell(bodyCell(safeText(row.name()), regular, TextAlignment.LEFT));
            table.addCell(bodyCell(safeText(row.studentNumber()), regular, TextAlignment.CENTER));
            table.addCell(bodyCell(resolveMarkText(row), regular, TextAlignment.CENTER));
            table.addCell(bodyCell(resolveRowDate(row), regular, TextAlignment.CENTER));
            table.addCell(bodyCell(safeText(row.teacherSignPlaceholder()), regular, TextAlignment.CENTER));
        }

        return table;
    }

    private void addEmptyRow(Table table, PdfFont regular) {
        for (int i = 0; i < 6; i++) {
            table.addCell(bodyCell("", regular, TextAlignment.CENTER));
        }
    }

    private Table buildDeanBlock(CourseProjectData data, PdfFont regular) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{28, 24, 48}))
                .useAllAvailableWidth();
        table.setMarginTop(12f);

        table.addCell(noBorderCell("Декан факультету", regular, TextAlignment.LEFT));
        table.addCell(signatureLine("", regular));
        table.addCell(signatureLine(safeText(data.dean()), regular));

        table.addCell(noBorderCell("", regular, TextAlignment.LEFT));
        table.addCell(hintCell("(підпис)", regular));
        table.addCell(hintCell("(прізвище,ініціали)", regular));
        return table;
    }

    private Table buildExaminerBlock(String teacherName, PdfFont regular, PdfFont bold) {
        Table signatureTable = new Table(UnitValue.createPercentArray(new float[]{20, 5, 20, 5, 20}))
                .useAllAvailableWidth();

        signatureTable.addCell(signatureLabelCell("Екзаменатор (Викладач)", bold));
        signatureTable.addCell(signatureSpacerCell());
        signatureTable.addCell(signatureLineCell("", bold));
        signatureTable.addCell(signatureSpacerCell());
        signatureTable.addCell(signatureLineCell(safeText(teacherName), bold));

        signatureTable.addCell(signatureSpacerCell());
        signatureTable.addCell(signatureSpacerCell());
        signatureTable.addCell(signatureHintCell("(підпис)", regular));
        signatureTable.addCell(signatureSpacerCell());
        signatureTable.addCell(signatureHintCell("(прізвище,ініціали)", regular));
        return signatureTable;
    }

    private Paragraph centered(String text, PdfFont font, float size) {
        return new Paragraph(text)
                .setFont(font)
                .setFontSize(size)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private Table buildCourseGroupRow(CourseProjectData data, PdfFont regular) {
        Table groupTable = new Table(UnitValue.createPercentArray(new float[]{25, 12, 20, 43}))
                .useAllAvailableWidth();

        groupTable.addCell(noBorderCell("Курс: ", regular, TextAlignment.LEFT));
        groupTable.addCell(underlinedCell(safeText(data.courseNumber()), regular));
        groupTable.addCell(noBorderCell("Група: ", regular, TextAlignment.RIGHT));
        groupTable.addCell(underlinedCell(safeText(data.groupName()), regular));
        return groupTable;
    }

    private Table buildDisciplineRow(CourseProjectData data, PdfFont regular) {
        Table disciplineTable = new Table(UnitValue.createPercentArray(new float[]{15, 70, 15}))
                .useAllAvailableWidth();
        disciplineTable.addCell(noBorderCell("з дисципліни: ", regular, TextAlignment.LEFT));
        disciplineTable.addCell(underlinedCell(safeText(data.disciplineName()), regular));
        disciplineTable.addCell(underlinedCell("", regular));
        disciplineTable.addCell(noBorderCell("", regular, TextAlignment.LEFT));
        disciplineTable.addCell(hintCell("(назва дисципліни)", regular));
        disciplineTable.addCell(noBorderCell("", regular, TextAlignment.LEFT));
        return disciplineTable;
    }

    private Table buildSemesterRow(CourseProjectData data, PdfFont regular) {
        Table semControlTable = new Table(UnitValue.createPercentArray(new float[]{10, 10, 80}))
                .useAllAvailableWidth();
        semControlTable.addCell(noBorderCell("за", regular, TextAlignment.LEFT));
        semControlTable.addCell(underlinedCell(safeText(data.semesterNumber()), regular));
        semControlTable.addCell(noBorderCell("-й навчальний семестр.", regular, TextAlignment.LEFT));
        return semControlTable;
    }

    private Table buildTeacherRow(String label, String value, String hintText, PdfFont font) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{10, 80, 10}))
                .useAllAvailableWidth();
        table.addCell(noBorderCell(label, font, TextAlignment.LEFT));
        table.addCell(underlinedCell(safeText(value), font));
        table.addCell(underlinedCell("", font));
        table.addCell(noBorderCell("", font, TextAlignment.LEFT));
        table.addCell(hintCell(hintText, font));
        table.addCell(noBorderCell("", font, TextAlignment.LEFT));
        return table;
    }

    private Cell underlinedCell(String value, PdfFont font) {
        return new Cell()
                .add(new Paragraph(value)
                        .setFont(font)
                        .setFontSize(DEFAULT_FONT_SIZE)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(BORDER_WIDTH))
                .setPadding(2f);
    }

    private Cell hintCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(8f)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER)
                .setPadding(0f);
    }

    private Cell noBorderCell(String text, PdfFont font, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(DEFAULT_FONT_SIZE)
                        .setTextAlignment(alignment))
                .setBorder(Border.NO_BORDER)
                .setPadding(2f);
    }

    private Cell headerCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(10f)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(new SolidBorder(BORDER_WIDTH))
                .setPadding(4f);
    }

    private Cell numberHeaderCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(10f)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(new SolidBorder(BORDER_WIDTH))
                .setPadding(3f);
    }

    private Cell bodyCell(String text, PdfFont font, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(10f)
                        .setTextAlignment(alignment))
                .setBorder(new SolidBorder(BORDER_WIDTH))
                .setPadding(4f);
    }

    private Cell signatureLine(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(safeText(text))
                        .setFont(font)
                        .setFontSize(10f)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(BORDER_WIDTH))
                .setPadding(2f);
    }

    private Cell signatureLabelCell(String label, PdfFont bold) {
        return new Cell()
                .add(new Paragraph(label)
                        .setFont(bold)
                        .setFontSize(10))
                .setBorder(Border.NO_BORDER)
                .setPadding(0);
    }

    private Cell signatureLineCell(String value, PdfFont font) {
        return new Cell()
                .add(new Paragraph(value)
                        .setFont(font)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(BORDER_WIDTH))
                .setPadding(0);
    }

    private Cell signatureHintCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(8)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER)
                .setPadding(0);
    }

    private Cell signatureSpacerCell() {
        return new Cell().setBorder(Border.NO_BORDER).setPadding(0);
    }

    private float getAvailableHeight(Document document, MeasuringDocumentRenderer renderer) {
        LayoutArea currentArea = renderer.getCurrentArea();
        if (currentArea == null) {
            return document.getPdfDocument().getDefaultPageSize().getHeight()
                    - document.getTopMargin() - document.getBottomMargin();
        }
        return currentArea.getBBox().getHeight();
    }

    private float measureHeight(Div element, Document document) {
        Rectangle measurementArea = new Rectangle(
                document.getPdfDocument().getDefaultPageSize().getWidth()
                        - document.getLeftMargin() - document.getRightMargin(),
                10_000);
        IRenderer renderer = element.createRendererSubTree();
        renderer.setParent(document.getRenderer());
        LayoutResult result = renderer.layout(new LayoutContext(new LayoutArea(1, measurementArea)));
        return result.getOccupiedArea().getBBox().getHeight();
    }

    private PdfFont loadFont(String resourcePath, String fallbackFont) throws IOException {
        try (InputStream fontStream = CourseProjectPdfGenerator.class.getResourceAsStream(resourcePath)) {
            if (fontStream != null) {
                FontProgram fontProgram = FontProgramFactory.createFont(fontStream.readAllBytes());
                return PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H);
            }
        } catch (IOException ex) {
            log.warn("Unable to load font {}: {}", resourcePath, ex.getMessage());
        }
        return PdfFontFactory.createFont(fallbackFont);
    }

    private Path resolveOutputPath(CourseProjectData data) {
        String fileName = safeText(data.groupName()) + "_course-project_"
                + safeText(data.semesterNumber()) + "_"
                + safeText(data.sheetNumber()) + ".pdf";
        return Paths.get("uploads").resolve(fileName);
    }

    private String resolveRowDate(StudentRow row) {
        if (row.date() != null) {
            return row.date().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
        return safeText(row.dateText());
    }

    private String resolveMarkText(StudentRow row) {
        if (!safeText(row.markText()).isBlank()) {
            return safeText(row.markText());
        }
        return safeText(row.mark());
    }

    private String formatDate(CourseProjectData data) {
        String day = Objects.toString(data.day(), "").trim();
        String month = Objects.toString(data.month(), "").trim();
        String year = Objects.toString(data.year(), "").trim();
        if (!day.isEmpty() && !month.isEmpty() && !year.isEmpty()) {
            return String.format("%s  %s  %s року", day, month, year);
        }
        String dateText = safeText(data.dateText());
        return dateText.isBlank() ? "" : dateText + " року";
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private LineSeparator createSeparator(SolidLine line) {
        LineSeparator separator = new LineSeparator(line);
        separator.setMarginTop(-6);
        separator.setMarginBottom(0);
        return separator;
    }

    private static class MeasuringDocumentRenderer extends DocumentRenderer {
        protected MeasuringDocumentRenderer(Document document) {
            super(document);
        }

        public LayoutArea getCurrentArea() {
            return super.getCurrentArea();
        }
    }

    private record CourseProjectData(
            String facultyName,
            String specialityName,
            String courseNumber,
            String groupName,
            String studyYear,
            String sheetNumber,
            String day,
            String month,
            String year,
            String dateText,
            String disciplineName,
            String semesterNumber,
            String teacherFullName1,
            String teacherFullName2,
            String dean,
            List<StudentRow> students
    ) {

        static CourseProjectData from(Object source) {
            if (source instanceof CourseProjectData data) {
                return data;
            }
            if (source instanceof DataModelForZalik zalik) {
                return new CourseProjectData(
                        zalik.facultyName(),
                        zalik.specialityName(),
                        zalik.courseNumber(),
                        zalik.groupName(),
                        zalik.studyYear(),
                        zalik.order(),
                        zalik.day(),
                        zalik.month(),
                        zalik.year(),
                        null,
                        zalik.disciplineName(),
                        zalik.semesterNumber(),
                        zalik.firstTeacher(),
                        zalik.secondTeacher(),
                        zalik.dean(),
                        toStudentRows(zalik.students())
                );
            }
            throw new DocumentException("Unsupported data type for course project PDF: " + source);
        }
    }

    private record StudentRow(
            int index,
            String name,
            String studentNumber,
            String markText,
            String mark,
            LocalDate date,
            String dateText,
            String teacherSignPlaceholder
    ) {
    }

    private static List<StudentRow> toStudentRows(List<StudentModelToDocumentGenerate> students) {
        if (students == null) {
            return Collections.emptyList();
        }
        List<StudentRow> rows = new ArrayList<>();
        for (StudentModelToDocumentGenerate student : students) {
            rows.add(new StudentRow(
                    student.index(),
                    student.name(),
                    student.studentNumber(),
                    student.nationalMark(),
                    student.mark(),
                    student.date(),
                    student.dateText(),
                    student.teacherSignPlaceholder()
            ));
        }
        return rows;
    }
}
