package com.esvar.dekanat.generate.pdf;

import com.esvar.dekanat.document.DocumentException;
import com.esvar.dekanat.document.PdfGenerator;
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
 * Base generator that renders CONTROL_WORK, COURSE_WORK and COURSE_PROJECT statements
 * with unified layout and pagination rules.
 */
public abstract class BaseStatementPdfGenerator implements PdfGenerator {

    private static final Logger log = LoggerFactory.getLogger(BaseStatementPdfGenerator.class);
    private static final float BORDER_WIDTH = 0.5f;
    private static final float DEFAULT_FONT_SIZE = 11f;
    private final DocumentType documentType;

    protected BaseStatementPdfGenerator(DocumentType documentType) {
        this.documentType = documentType;
    }

    protected DocumentType getDocumentType() {
        return documentType;
    }

    @Override
    public Path generatePdf(Object data) {
        StatementDocumentData statementData = StatementDocumentData.from(data);

        try {
            Path outputPath = resolveOutputPath(statementData);
            Files.createDirectories(outputPath.getParent());

            try (PdfWriter writer = new PdfWriter(outputPath.toFile());
                 PdfDocument pdfDocument = new PdfDocument(writer);
                 Document document = new Document(pdfDocument, PageSize.A4)) {

                PdfFont regular = loadFont("/fonts/times.ttf", StandardFonts.TIMES_ROMAN);
                PdfFont bold = loadFont("/fonts/timesbd.ttf", StandardFonts.TIMES_BOLD);

                MeasuringDocumentRenderer renderer = new MeasuringDocumentRenderer(document);
                document.setRenderer(renderer);
                document.setFont(regular);

                addHeader(document, statementData, regular, bold);
                addStudentsAndFooterWithGlue(document, statementData, regular, bold, renderer);

                log.info("Generated {} PDF at {}", getName(), outputPath);
            }

            return outputPath;
        } catch (IOException e) {
            throw new DocumentException("Failed to generate PDF", e);
        }
    }

    private void addHeader(Document document, StatementDocumentData data, PdfFont regular, PdfFont bold) {
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
        document.add(centered(data.formattedDate(), bold, DEFAULT_FONT_SIZE).setUnderline());

        document.add(buildDisciplineRow(data, regular));
        document.add(buildSemesterRow(data, regular));
        document.add(buildTeacherRow("Викладач", data.teacherFullName(), regular));
        document.add(new Paragraph(" "));
    }

    private void addStudentsAndFooterWithGlue(Document document,
                                              StatementDocumentData data,
                                              PdfFont regular,
                                              PdfFont bold,
                                              MeasuringDocumentRenderer renderer) {
        List<StudentModelToDocumentGenerate> students =
                data.students() == null ? Collections.emptyList() : data.students();

        if (students.size() > 1) {
            Table mainTable = buildStudentsTable(students.subList(0, students.size() - 1), regular, bold);
            document.add(mainTable);
        }

        List<StudentModelToDocumentGenerate> lastRows = students.isEmpty()
                ? Collections.emptyList()
                : List.of(students.get(students.size() - 1));

        Div tailSection = new Div();
        tailSection.add(buildStudentsTable(lastRows, regular, bold));
        tailSection.add(buildFooter(data, regular, bold));

        float remainingHeight = remainingHeight(renderer, document);
        float tailHeight = measureHeight(tailSection, document);

        if (tailHeight > remainingHeight) {
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        }

        document.add(tailSection);
    }

    private Div buildFooter(StatementDocumentData data, PdfFont regular, PdfFont bold) {
        Div footer = new Div();
        footer.add(buildDeanBlock(data, regular));
        if (documentType.includeTeacherSignature()) {
            footer.add(new Paragraph(" "));
            footer.add(buildTeacherSignatureBlock(data.teacherFullName(), regular, bold));
        }
        return footer;
    }

    private Table buildStudentsTable(List<StudentModelToDocumentGenerate> rows, PdfFont regular, PdfFont bold) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{7, 34, 18, 18, 12, 11}))
                .useAllAvailableWidth();
//        table.setHeaderRows(2);

        table.addHeaderCell(headerCell("№ з/п", bold));
        table.addHeaderCell(headerCell("Прізвище та ініціали студента", bold));
        table.addHeaderCell(headerCell("№ залікової книжки", bold));
        table.addHeaderCell(headerCell(documentType.markColumnHeader(), bold));
        table.addHeaderCell(headerCell("Дата", bold));
        table.addHeaderCell(headerCell("Підпис викладача", bold));

        for (int i = 1; i <= 6; i++) {
            table.addHeaderCell(numberHeaderCell(String.valueOf(i), regular));
        }

        if (rows.isEmpty()) {
            addEmptyRow(table, regular);
            return table;
        }

        for (StudentModelToDocumentGenerate row : rows) {
            table.addCell(bodyCell(row.index() + ".", regular, TextAlignment.CENTER));
            table.addCell(bodyCell(safeText(row.name()), regular, TextAlignment.LEFT));
            table.addCell(bodyCell(safeText(row.studentNumber()), regular, TextAlignment.CENTER));
            table.addCell(bodyCell(documentType.resolveMark(row), regular, TextAlignment.CENTER));
            table.addCell(bodyCell(resolveDate(row), regular, TextAlignment.CENTER));
            table.addCell(bodyCell(safeText(row.teacherSignPlaceholder()), regular, TextAlignment.CENTER));
        }

        return table;
    }

    private void addEmptyRow(Table table, PdfFont regular) {
        for (int i = 0; i < 6; i++) {
            table.addCell(bodyCell("", regular, TextAlignment.CENTER));
        }
    }

    private Table buildDeanBlock(StatementDocumentData data, PdfFont regular) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{28, 24, 48}))
                .useAllAvailableWidth();
        table.setMarginTop(12f);

        table.addCell(noBorderCell("Декан факультету", regular, TextAlignment.LEFT));
        table.addCell(signatureLine("", regular));
        table.addCell(signatureLine(safeText(data.headName()), regular));

        table.addCell(noBorderCell("", regular, TextAlignment.LEFT));
        table.addCell(hintCell("(підпис)", regular));
        table.addCell(hintCell("(прізвище,ініціали)", regular));

        return table;
    }

    private Table buildTeacherSignatureBlock(String teacherName, PdfFont regular, PdfFont bold) {
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

    private Table buildCourseGroupRow(StatementDocumentData data, PdfFont regular) {
        Table groupTable = new Table(UnitValue.createPercentArray(new float[]{25, 12, 20, 43}))
                .useAllAvailableWidth();

        groupTable.addCell(noBorderCell("Курс: ", regular, TextAlignment.LEFT));
        groupTable.addCell(underlinedCell(safeText(data.courseNumber()), regular));
        groupTable.addCell(noBorderCell("Група: ", regular, TextAlignment.RIGHT));
        groupTable.addCell(underlinedCell(safeText(data.groupName()), regular));
        return groupTable;
    }

    private Table buildDisciplineRow(StatementDocumentData data, PdfFont regular) {
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

    private Table buildSemesterRow(StatementDocumentData data, PdfFont regular) {
        Table semControlTable = new Table(UnitValue.createPercentArray(new float[]{10, 10, 80}))
                .useAllAvailableWidth();
        semControlTable.addCell(noBorderCell("за", regular, TextAlignment.LEFT));
        semControlTable.addCell(underlinedCell(safeText(data.semesterNumber()), regular));
        semControlTable.addCell(noBorderCell("-й навчальний семестр.", regular, TextAlignment.LEFT));
        return semControlTable;
    }

    private Table buildTeacherRow(String label, String value, PdfFont font) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{10, 80, 10}))
                .useAllAvailableWidth();
        table.addCell(noBorderCell(label, font, TextAlignment.LEFT));
        table.addCell(underlinedCell(safeText(value), font));
        table.addCell(underlinedCell("", font));
        table.addCell(noBorderCell("", font, TextAlignment.LEFT));
        table.addCell(hintCell("(прізвище, ім’я та по батькові викладача)", font));
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

    private float remainingHeight(MeasuringDocumentRenderer renderer, Document document) {
        LayoutArea currentArea = renderer.getCurrentArea();
        if (currentArea == null) {
            return document.getPdfDocument().getDefaultPageSize().getHeight()
                    - document.getTopMargin() - document.getBottomMargin();
        }
        return currentArea.getBBox().getHeight();
    }

    private float measureHeight(Div element, Document document) {
        Rectangle measurementArea = new Rectangle(
                document.getPdfDocument().getDefaultPageSize().getWidth() - document.getLeftMargin() - document.getRightMargin(),
                10_000);
        IRenderer renderer = element.createRendererSubTree();
        renderer.setParent(document.getRenderer());
        LayoutResult result = renderer.layout(new LayoutContext(new LayoutArea(1, measurementArea)));
        return result.getOccupiedArea().getBBox().getHeight();
    }

    private PdfFont loadFont(String resourcePath, String fallbackFont) throws IOException {
        try (InputStream fontStream = BaseStatementPdfGenerator.class.getResourceAsStream(resourcePath)) {
            if (fontStream != null) {
                FontProgram fontProgram = FontProgramFactory.createFont(fontStream.readAllBytes());
                return PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H);
            }
        } catch (IOException ex) {
            log.warn("Unable to load font {}: {}", resourcePath, ex.getMessage());
        }
        return PdfFontFactory.createFont(fallbackFont);
    }

    protected Path resolveOutputPath(StatementDocumentData data) {
        String fileName = safeText(data.groupName()) + "_"
                + documentType.outputSuffix() + "_"
                + safeText(data.semesterNumber()) + "_"
                + safeText(data.sheetNumber()) + ".pdf";
        return Paths.get("uploads").resolve(fileName);
    }

    private static String resolveDate(StudentModelToDocumentGenerate row) {
        if (row.date() != null) {
            return formatDate(row.date());
        }
        return safeText(row.dateText());
    }

    private static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private LineSeparator createSeparator(SolidLine line) {
        LineSeparator separator = new LineSeparator(line);
        separator.setMarginTop(-6);
        separator.setMarginBottom(0);
        return separator;
    }

    /**
     * Renderer exposing the current layout area height for glue calculations.
     */
    private static class MeasuringDocumentRenderer extends DocumentRenderer {
        protected MeasuringDocumentRenderer(Document document) {
            super(document);
        }

        public LayoutArea getCurrentArea() {
            return super.getCurrentArea();
        }
    }
}
