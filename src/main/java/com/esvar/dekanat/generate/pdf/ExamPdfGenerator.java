package com.esvar.dekanat.generate.pdf;

import com.esvar.dekanat.document.DocumentException;
import com.esvar.dekanat.document.PdfGenerator;
import com.esvar.dekanat.generate.DataModelForExam;
import com.esvar.dekanat.generate.ExamStudentRow;
import com.esvar.dekanat.generate.ExamSummary;
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
import java.util.Objects;

/**
 * PDF generator for exam grade sheets.
 */
@Component
public class ExamPdfGenerator implements PdfGenerator {

    public static final String NAME = "exam";

    private static final Logger log = LoggerFactory.getLogger(ExamPdfGenerator.class);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Path generatePdf(Object data) {
        if (!(data instanceof DataModelForExam examData)) {
            throw new DocumentException("Expected DataModelForExam");
        }

        try {
            Path outputPath = resolveOutputPath(examData);
            Files.createDirectories(outputPath.getParent());

            try (PdfWriter writer = new PdfWriter(outputPath.toFile());
                 PdfDocument pdfDocument = new PdfDocument(writer);
                 Document document = new Document(pdfDocument, PageSize.A4)) {

                PdfFont regular = loadFont("/fonts/times.ttf", StandardFonts.TIMES_ROMAN);
                PdfFont bold = loadFont("/fonts/timesbd.ttf", StandardFonts.TIMES_BOLD);

                document.setFont(regular);

                addHeader(document, regular, bold, examData);
                addStudentsTable(document, regular, bold, examData);
                addSummarySection(document, regular, bold, examData.summary());
                addFooter(document, regular, bold, examData);

                log.info("Generated exam PDF at {}", outputPath);
            }

            return outputPath;
        } catch (IOException e) {
            throw new DocumentException("Failed to generate exam PDF", e);
        }
    }

    private void addHeader(Document document, PdfFont regular, PdfFont bold, DataModelForExam data) {
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

        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{20, 10, 20, 10, 20, 20}))
                .useAllAvailableWidth();
        infoTable.addCell(buildLabelCell("Курс: ", regular));
        infoTable.addCell(buildValueCell(Objects.toString(data.courseNumber(), ""), regular));
        infoTable.addCell(buildLabelCell("\u00A0\u00A0\u00A0\u00A0Група: ", regular));
        infoTable.addCell(buildValueCell(Objects.toString(data.groupName(), ""), regular));
        infoTable.addCell(buildLabelCell("\u00A0\u00A0\u00A0\u00A0Навчальний рік: ", regular));
        infoTable.addCell(buildValueCell(Objects.toString(data.studyYear(), ""), regular));
        document.add(infoTable);

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

        document.add(new Paragraph("Форма семестрового контролю: "
                        + Objects.toString(data.controlTypeName(), "")
                        + " \u200B" + Objects.toString(data.hours(), "") + " навчальних годин")
                .setFont(regular)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Проведення підсумкового контролю ________________________________")
                .setFont(regular)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Викладач: " + Objects.toString(data.firstTeacher(), ""))
                .setFont(regular)
                .setFontSize(11));
        document.add(new Paragraph("Другий викладач: " + Objects.toString(data.secondTeacher(), ""))
                .setFont(regular)
                .setFontSize(11));
        document.add(new Paragraph(""));
    }

    private void addStudentsTable(Document document, PdfFont regular, PdfFont bold, DataModelForExam data) {
        float[] widths = {4, 12, 12, 12, 12, 12, 10, 8, 8, 8, 8, 6, 10};
        Table table = new Table(UnitValue.createPercentArray(widths)).useAllAvailableWidth();

        table.addHeaderCell(createHeaderCell("№", bold, 2, 1));
        table.addHeaderCell(createHeaderCell("Прізвище", bold, 2, 1));
        table.addHeaderCell(createHeaderCell("Ім'я", bold, 2, 1));
        table.addHeaderCell(createHeaderCell("По батькові", bold, 2, 1));
        table.addHeaderCell(createHeaderCell("Номер залікової книжки", bold, 2, 1));
        table.addHeaderCell(createHeaderCell("Галузь знань", bold, 2, 1));
        table.addHeaderCell(createHeaderCell("Спеціальність", bold, 2, 1));
        table.addHeaderCell(createHeaderCell("Поточний контроль", bold, 1, 2));
        table.addHeaderCell(createHeaderCell("Підсумковий контроль", bold, 2, 1));
        table.addHeaderCell(createHeaderCell("Сума балів", bold, 2, 1));
        table.addHeaderCell(createHeaderCell("ОЦІНКА ECTS", bold, 2, 1));
        table.addHeaderCell(createHeaderCell("ОЦІНКА ЗА НАЦІОНАЛЬНОЮ ШКАЛОЮ", bold, 2, 1));

        table.addHeaderCell(createHeaderCell("МК1", bold, 1, 1));
        table.addHeaderCell(createHeaderCell("МК2", bold, 1, 1));

        for (ExamStudentRow student : data.students()) {
            table.addCell(defaultCell(String.valueOf(student.index()), regular, TextAlignment.CENTER));
            table.addCell(defaultCell(student.surname(), regular, TextAlignment.LEFT));
            table.addCell(defaultCell(student.name(), regular, TextAlignment.LEFT));
            table.addCell(defaultCell(student.patronymic(), regular, TextAlignment.LEFT));
            table.addCell(defaultCell(student.recordBookNumber(), regular, TextAlignment.CENTER));
            table.addCell(defaultCell(student.knowledgeArea(), regular, TextAlignment.CENTER));
            table.addCell(defaultCell(student.speciality(), regular, TextAlignment.CENTER));
            table.addCell(defaultCell(student.firstModuleMark(), regular, TextAlignment.CENTER));
            table.addCell(defaultCell(student.secondModuleMark(), regular, TextAlignment.CENTER));
            table.addCell(defaultCell(student.finalControlMark(), regular, TextAlignment.CENTER));
            table.addCell(defaultCell(student.totalPoints(), regular, TextAlignment.CENTER));
            table.addCell(defaultCell(student.ectsGrade(), regular, TextAlignment.CENTER));
            table.addCell(defaultCell(student.nationalGrade(), regular, TextAlignment.CENTER));
        }

        document.add(table);
    }

    private void addSummarySection(Document document, PdfFont regular, PdfFont bold, ExamSummary summary) {
        document.add(new Paragraph(""));

        float[] widths = {10, 10, 10, 10, 10, 10, 10};
        Table summaryTable = new Table(UnitValue.createPercentArray(widths)).useAllAvailableWidth();
        summaryTable.addHeaderCell(createHeaderCell("90-100", bold, 1, 1));
        summaryTable.addHeaderCell(createHeaderCell("82-89", bold, 1, 1));
        summaryTable.addHeaderCell(createHeaderCell("74-81", bold, 1, 1));
        summaryTable.addHeaderCell(createHeaderCell("64-73", bold, 1, 1));
        summaryTable.addHeaderCell(createHeaderCell("60-63", bold, 1, 1));
        summaryTable.addHeaderCell(createHeaderCell("35-59", bold, 1, 1));
        summaryTable.addHeaderCell(createHeaderCell("1-34", bold, 1, 1));

        summaryTable.addCell(defaultCell(summary.countA(), regular, TextAlignment.CENTER));
        summaryTable.addCell(defaultCell(summary.countB(), regular, TextAlignment.CENTER));
        summaryTable.addCell(defaultCell(summary.countC(), regular, TextAlignment.CENTER));
        summaryTable.addCell(defaultCell(summary.countD(), regular, TextAlignment.CENTER));
        summaryTable.addCell(defaultCell(summary.countE(), regular, TextAlignment.CENTER));
        summaryTable.addCell(defaultCell(summary.countFx(), regular, TextAlignment.CENTER));
        summaryTable.addCell(defaultCell(summary.countF(), regular, TextAlignment.CENTER));

        document.add(summaryTable);

        document.add(new Paragraph("Середній бал за національною шкалою: " + summary.averageNationalScore())
                .setFont(regular)
                .setFontSize(11));
        document.add(new Paragraph("Середній бал ECTS: " + summary.averageEctsScore())
                .setFont(regular)
                .setFontSize(11));
        document.add(new Paragraph("Середня сума балів: " + summary.averagePoints())
                .setFont(regular)
                .setFontSize(11));
        document.add(new Paragraph("Кількість студентів: " + summary.totalStudents())
                .setFont(regular)
                .setFontSize(11));
    }

    private void addFooter(Document document, PdfFont regular, PdfFont bold, DataModelForExam data) {
        document.add(new Paragraph("Дата проведення підсумкового екзамену (заліку) "
                        + Objects.toString(data.day(), "") + "."
                        + Objects.toString(data.month(), "") + "."
                        + Objects.toString(data.year(), ""))
                .setFont(regular)
                .setFontSize(11));

        document.add(new Paragraph(""));

        Table signTable = new Table(UnitValue.createPercentArray(new float[]{33, 34, 33}))
                .useAllAvailableWidth();
        signTable.addCell(signatureCell("Екзаменатор (викладач)", Objects.toString(data.gradeTeacher(), ""), regular));
        signTable.addCell(signatureCell("Завідувач кафедри", Objects.toString(data.departmentHead(), ""), regular));
        signTable.addCell(signatureCell("Декан факультету", Objects.toString(data.dean(), ""), regular));

        document.add(signTable);
    }

    private Cell createHeaderCell(String text, PdfFont font, int rowSpan, int colSpan) {
        Cell cell = new Cell(rowSpan, colSpan)
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(9)
                        .setTextAlignment(TextAlignment.CENTER))
                .setPadding(4);
        return cell;
    }

    private Cell defaultCell(String text, PdfFont font, TextAlignment alignment) {
        return new Cell().add(new Paragraph(text == null ? "" : text)
                        .setFont(font)
                        .setFontSize(9)
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

    private PdfFont loadFont(String resourcePath, String fallbackFont) throws IOException {
        try (InputStream fontStream = ExamPdfGenerator.class.getResourceAsStream(resourcePath)) {
            if (fontStream != null) {
                FontProgram fontProgram = FontProgramFactory.createFont(fontStream.readAllBytes());
                return PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H);
            }
        } catch (IOException ex) {
            log.warn("Unable to load font {}: {}", resourcePath, ex.getMessage());
        }
        return PdfFontFactory.createFont(fallbackFont);
    }

    private Path resolveOutputPath(DataModelForExam data) {
        String fileName = Objects.toString(data.groupName(), "group") + "_exam_"
                + Objects.toString(data.semesterNumber(), "0") + "_"
                + Objects.toString(data.order(), "00") + ".pdf";
        return Paths.get("uploads").resolve(fileName);
    }
}
