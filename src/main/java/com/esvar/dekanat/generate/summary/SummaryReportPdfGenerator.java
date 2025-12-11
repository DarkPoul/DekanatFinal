package com.esvar.dekanat.generate.summary;

import com.esvar.dekanat.document.PdfGenerator;
import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;

import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;


import org.springframework.stereotype.Component;

@Component
public class SummaryReportPdfGenerator {

    private static final float FIRST_COLUMN_WIDTH = 130f;
    private static final float OTHER_COLUMN_WIDTH = 18f;
    private static final float TWO_MODULE_FIRST_COLUMN_WIDTH = 120f;
    private static final float TWO_MODULE_OTHER_COLUMN_WIDTH = 18f;
    private static final float TWO_MODULE_HEADER_ROW_HEIGHT = 90f;
    private static final float HEADER_ROW_HEIGHT = 110f;
    private static final String FIRST_MODULE_TITLE = "за перший модульний контроль";
    private static final String TWO_MODULE_TITLE = "за два модульних контролі";

    // ЗАЛИШАЄМО твою існуючу сигнатуру як-є для зворотної сумісності:
    public byte[] generateSummaryReport(
            String groupName,
            List<String> studentFullNames,
            List<DisciplineColumn> disciplineColumns,
            Map<String, List<Integer>> marksByStudent,
            boolean addAverageRow
    ) {
        // делегуємо в новий оверлоад без футера
        return generateSummaryReport(
                groupName,
                studentFullNames,
                disciplineColumns,
                marksByStudent,
                addAverageRow,
                null,                // examiner
                false,               // numericHeader
                false,               // includeSignature
                FIRST_MODULE_TITLE   // controlTitle
        );
    }

    public byte[] generateTwoModuleSummaryReport(
            String groupName,
            List<String> studentFullNames,
            List<DisciplineColumn> disciplineColumns,
            Map<String, List<ModuleMark>> marksByStudent,
            boolean addAverageRow,
            String examiner,
            boolean numericHeader,
            boolean includeSignature
    ) {
        return generateTwoModuleSummaryReport(
                groupName,
                studentFullNames,
                disciplineColumns,
                marksByStudent,
                addAverageRow,
                examiner,
                numericHeader,
                includeSignature,
                TWO_MODULE_TITLE
        );
    }

    public byte[] generateTwoModuleSummaryReport(
            String groupName,
            List<String> studentFullNames,
            List<DisciplineColumn> disciplineColumns,
            Map<String, List<ModuleMark>> marksByStudent,
            boolean addAverageRow,
            String examiner,
            boolean numericHeader,
            boolean includeSignature,
            String controlTitle
    ) {
        System.out.println("[SummaryReportPdfGenerator] Старт генерації PDF для групи (2 модулі): " + groupName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDocument = new PdfDocument(writer);
             Document document = new Document(pdfDocument, PageSize.A4.rotate())) {

            document.setMargins(20, 20, 20, 20);
            document.setFont(createFont());
            System.out.println("[SummaryReportPdfGenerator] Створено документ і встановлено шрифт");

            Paragraph title = new Paragraph(
                    "Зведений звіт результатів оцінювання студентів групи " + groupName +
                            " " + controlTitle)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(14)
                    .setMarginBottom(10);
            document.add(title);
            System.out.println("[SummaryReportPdfGenerator] Додано заголовок (2 модулі)");

        Table table = createTwoModuleTableStructure(disciplineColumns.size());
        addTwoModuleHeaderRows(table, disciplineColumns);
        addTwoModuleStudentRows(table, studentFullNames, disciplineColumns, marksByStudent);
            System.out.println("[SummaryReportPdfGenerator] Додано рядки студентів (2 модулі)");

            if (addAverageRow) {
                addTwoModuleZeroSummaryRow(table, studentFullNames, disciplineColumns, marksByStudent);
                System.out.println("[SummaryReportPdfGenerator] Додано підсумковий рядок по нулям (2 модулі)");
            }

            document.add(table);
            System.out.println("[SummaryReportPdfGenerator] Таблицю додано до документу (2 модулі)");

            if (includeSignature) {
                Table lastRowTable = createTwoModuleTableStructure(disciplineColumns.size());
                lastRowTable.setWidth(UnitValue.createPercentValue(100));
                lastRowTable.setFixedLayout();

                Div footer = generate(examiner, lastRowTable, numericHeader);
                document.add(footer);
                System.out.println("[SummaryReportPdfGenerator] Додано футер (numericHeader=" + numericHeader
                        + ", includeSignature=true) для 2 модулів");
            } else {
                System.out.println("[SummaryReportPdfGenerator] Футер з підписом пропущено (2 модулі)");
            }

        } catch (IOException e) {
            System.out.println("[SummaryReportPdfGenerator] Помилка генерації PDF (2 модулі): " + e.getMessage());
            throw new IllegalStateException("Не вдалося згенерувати PDF звіт", e);
        }

        System.out.println("[SummaryReportPdfGenerator] Генерація PDF завершена (2 модулі)");
        return baos.toByteArray();
    }

    // НОВИЙ ОВЕРЛОАД: з екзаменатором та прапорцем numericHeader
    public byte[] generateSummaryReport(
            String groupName,
            List<String> studentFullNames,
            List<DisciplineColumn> disciplineColumns,
            Map<String, List<Integer>> marksByStudent,
            boolean addAverageRow,
            String examiner,
            boolean numericHeader
    ) {
        boolean includeSignature = examiner != null && !examiner.isBlank();
        return generateSummaryReport(
                groupName,
                studentFullNames,
                disciplineColumns,
                marksByStudent,
                addAverageRow,
                examiner,
                numericHeader,
                includeSignature,
                FIRST_MODULE_TITLE
        );
    }

    public byte[] generateSummaryReport(
            String groupName,
            List<String> studentFullNames,
            List<DisciplineColumn> disciplineColumns,
            Map<String, List<Integer>> marksByStudent,
            boolean addAverageRow,
            String examiner,
            boolean numericHeader,
            boolean includeSignature,
            String controlTitle
    ) {
        System.out.println("[SummaryReportPdfGenerator] Старт генерації PDF для групи: " + groupName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDocument = new PdfDocument(writer);
             Document document = new Document(pdfDocument, PageSize.A4.rotate())) {

            document.setMargins(20, 20, 20, 20);
            document.setFont(createFont());
            System.out.println("[SummaryReportPdfGenerator] Створено документ і встановлено шрифт");

            Paragraph title = new Paragraph(
                    "Зведений звіт результатів оцінювання студентів групи " + groupName +
                            " " + controlTitle)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(14)
                    .setMarginBottom(10);
            document.add(title);
            System.out.println("[SummaryReportPdfGenerator] Додано заголовок");

            Table table = createTableStructure(disciplineColumns.size());
            addHeaderRows(table, disciplineColumns);
            addStudentRows(table, studentFullNames, disciplineColumns, marksByStudent);
            System.out.println("[SummaryReportPdfGenerator] Додано рядки студентів");

            if (addAverageRow) {
                addZeroSummaryRow(table, studentFullNames, disciplineColumns, marksByStudent);
                System.out.println("[SummaryReportPdfGenerator] Додано підсумковий рядок по нулям");
            }

            document.add(table);
            System.out.println("[SummaryReportPdfGenerator] Таблицю додано до документу");

            if (includeSignature) {
                Table lastRowTable = createTableStructure(disciplineColumns.size());
                lastRowTable.setWidth(UnitValue.createPercentValue(100));
                lastRowTable.setFixedLayout();

                Div footer = generate(examiner, lastRowTable, numericHeader);
                document.add(footer);
                System.out.println("[SummaryReportPdfGenerator] Додано футер (numericHeader=" + numericHeader
                        + ", includeSignature=true)");
            } else {
                System.out.println("[SummaryReportPdfGenerator] Футер з підписом пропущено");
            }

        } catch (IOException e) {
            System.out.println("[SummaryReportPdfGenerator] Помилка генерації PDF: " + e.getMessage());
            throw new IllegalStateException("Не вдалося згенерувати PDF звіт", e);
        }

        System.out.println("[SummaryReportPdfGenerator] Генерація PDF завершена");
        return baos.toByteArray();
    }

    private PdfFont createFont() throws IOException {
        try (InputStream fontStream = PdfGenerator.class.getResourceAsStream("/fonts/times.ttf")) {
            if (fontStream != null) {
                System.out.println("[SummaryReportPdfGenerator] Завантажуємо користувацький шрифт");
                FontProgram fontProgram = FontProgramFactory.createFont(fontStream.readAllBytes());
                return PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H);
            }
        } catch (IOException ex) {
            // fall back to default font below
            System.out.println("[SummaryReportPdfGenerator] Не вдалося завантажити користувацький шрифт, використаємо дефолтний");
        }
        System.out.println("[SummaryReportPdfGenerator] Використовується стандартний шрифт");
        return PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
    }

    private Table createTableStructure(int disciplineCount) {
        System.out.println("[SummaryReportPdfGenerator] Створюємо структуру таблиці. Кількість дисциплін: " + disciplineCount);
        int totalColumns = 2 + disciplineCount;
        float[] columnWidths = createAdjustedColumnWidths(
                totalColumns,
                TWO_MODULE_FIRST_COLUMN_WIDTH,
                TWO_MODULE_OTHER_COLUMN_WIDTH
        );

        Table table = new Table(columnWidths);
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFixedLayout();
        table.setFontSize(9f);
        System.out.println("[SummaryReportPdfGenerator] Таблиця створена з " + totalColumns + " колонками");
        return table;
    }

    private Table createTwoModuleTableStructure(int disciplineCount) {
        System.out.println("[SummaryReportPdfGenerator] Створюємо структуру таблиці (2 модулі). Кількість дисциплін: "
                + disciplineCount);
        int totalColumns = 2 + disciplineCount * 3;
        float[] columnWidths = createAdjustedColumnWidths(totalColumns, FIRST_COLUMN_WIDTH, OTHER_COLUMN_WIDTH);

        Table table = new Table(columnWidths);
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFixedLayout();
        System.out.println("[SummaryReportPdfGenerator] Таблиця (2 модулі) створена з " + totalColumns + " колонками");
        return table;
    }

    private float[] createAdjustedColumnWidths(int totalColumns, float originalFirstWidth, float otherWidth) {
        float reducedFirstWidth = originalFirstWidth * 0.6f;
        float freedWidth = originalFirstWidth - reducedFirstWidth;
        float additionalPerOther = freedWidth / (totalColumns - 1);
        float adjustedOtherWidth = otherWidth + additionalPerOther;

        float[] columnWidths = new float[totalColumns];
        columnWidths[0] = reducedFirstWidth;
        for (int i = 1; i < totalColumns; i++) {
            columnWidths[i] = adjustedOtherWidth;
        }
        return columnWidths;
    }

    private void addHeaderRows(Table table, List<DisciplineColumn> disciplineColumns) {
        System.out.println("[SummaryReportPdfGenerator] Додаємо заголовки колонок");
        int disciplineCount = disciplineColumns.size();

        Cell blankForName = new Cell().setBorder(new SolidBorder(1));
        table.addHeaderCell(blankForName);

        Cell disciplinesHeader = new Cell(1, disciplineCount)
                .add(new Paragraph("Дисципліна (макс. кількість балів)")
                        .setTextAlignment(TextAlignment.CENTER))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(1));
        table.addHeaderCell(disciplinesHeader);

        Cell blankForZeroColumn = new Cell().setBorder(new SolidBorder(1));
        table.addHeaderCell(blankForZeroColumn);

        Cell nameHeader = new Cell()
                .add(new Paragraph("ПІБ")
                        .setTextAlignment(TextAlignment.CENTER))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(1)).setFontSize(9f);
        table.addHeaderCell(nameHeader);

        for (DisciplineColumn discipline : disciplineColumns) {
            String headerText = discipline.title();
            if (discipline.elective()) {
                headerText = headerText + "\n(вибіркова)";
                System.out.println("[SummaryReportPdfGenerator] Дисципліна вибіркова: " + discipline.title());
            }

            Paragraph disciplineTitle = new Paragraph(headerText)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(9f);

            Cell disciplineHeaderCell = new Cell()
                    .add(disciplineTitle)
                    .setMinHeight(HEADER_ROW_HEIGHT)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setBorder(new SolidBorder(1));
            table.addHeaderCell(disciplineHeaderCell);
        }

        Paragraph zeroColumnText = new Paragraph("К-сть 0 у студента")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(9f);

        Cell zeroHeaderCell = new Cell()
                .add(zeroColumnText)
                .setMinHeight(HEADER_ROW_HEIGHT)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(1));
        table.addHeaderCell(zeroHeaderCell);
        System.out.println("[SummaryReportPdfGenerator] Заголовки колонок додано");
    }

    private void addTwoModuleHeaderRows(Table table, List<DisciplineColumn> disciplineColumns) {
        System.out.println("[SummaryReportPdfGenerator] Додаємо заголовки колонок (2 модулі)");

        Cell nameHeader = new Cell(2, 1)
                .add(new Paragraph("ПІБ").setTextAlignment(TextAlignment.CENTER))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(1)).setFontSize(9f);
        table.addHeaderCell(nameHeader);

        for (DisciplineColumn discipline : disciplineColumns) {
            String headerText = discipline.title();
            if (discipline.elective()) {
                headerText = headerText + "\n(вибіркова)";
                System.out.println("[SummaryReportPdfGenerator] Дисципліна вибіркова: " + discipline.title());
            }

            Paragraph disciplineTitle = new Paragraph(headerText)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(9f);

            Cell disciplineHeaderCell = new Cell(1, 3)
                    .add(disciplineTitle)
                    .setMinHeight(TWO_MODULE_HEADER_ROW_HEIGHT)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setBorder(new SolidBorder(1));
            table.addHeaderCell(disciplineHeaderCell);
        }

        Cell zeroHeader = new Cell(2, 1)
                .add(new Paragraph("К-сть 0 у студента").setTextAlignment(TextAlignment.CENTER).setFontSize(9f))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(1));
        table.addHeaderCell(zeroHeader);

        for (int i = 0; i < disciplineColumns.size(); i++) {
            table.addHeaderCell(createSubHeaderCell("1м"));
            table.addHeaderCell(createSubHeaderCell("2м"));
            table.addHeaderCell(createSubHeaderCell("С"));
        }

        System.out.println("[SummaryReportPdfGenerator] Заголовки колонок додано (2 модулі)");
    }

    private Cell createSubHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(8f))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(1));
    }

    private void addStudentRows(Table table,
                                List<String> studentFullNames,
                                List<DisciplineColumn> disciplineColumns,
                                Map<String, List<Integer>> marksByStudent) {
        System.out.println("[SummaryReportPdfGenerator] Додаємо рядки студентів: " + studentFullNames.size());
        for (String student : studentFullNames) {
            Cell nameCell = new Cell()
                    .add(new Paragraph(student))
                    .setTextAlignment(TextAlignment.LEFT)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setBorder(new SolidBorder(1));
            table.addCell(nameCell);

            List<Integer> marks = marksByStudent.getOrDefault(student, Collections.emptyList());
            int zeroCount = 0;

            for (int i = 0; i < disciplineColumns.size(); i++) {
                Integer mark = getMark(marks, i);
                if (mark != null && mark == 0) {
                    zeroCount++;
                }

                table.addCell(createMarkCell(mark));
            }

            table.addCell(createNumericCell(zeroCount));
            System.out.println("[SummaryReportPdfGenerator] Додано рядок студента: " + student + ", кількість нулів: " + zeroCount);
        }
    }

    private void addTwoModuleStudentRows(Table table,
                                         List<String> studentFullNames,
                                         List<DisciplineColumn> disciplineColumns,
                                         Map<String, List<ModuleMark>> marksByStudent) {
        System.out.println("[SummaryReportPdfGenerator] Додаємо рядки студентів (2 модулі): " + studentFullNames.size());
        for (String student : studentFullNames) {
            Cell nameCell = new Cell()
                    .add(new Paragraph(student))
                    .setTextAlignment(TextAlignment.LEFT)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setBorder(new SolidBorder(1)).setFontSize(8f);
            table.addCell(nameCell);

            List<ModuleMark> marks = marksByStudent.getOrDefault(student, Collections.emptyList());
            int zeroCount = 0;

            for (int i = 0; i < disciplineColumns.size(); i++) {
                ModuleMark moduleMark = getModuleMark(marks, i);
                Integer first = moduleMark == null ? null : moduleMark.firstModule();
                Integer second = moduleMark == null ? null : moduleMark.secondModule();
                Integer total = moduleMark == null ? null : moduleMark.totalOrNull();

                table.addCell(createMarkCell(first));
                table.addCell(createMarkCell(second));
                table.addCell(createMarkCell(total));

                if (moduleMark != null && moduleMark.hasAnyMark() && moduleMark.totalOrNull() != null
                        && moduleMark.totalOrNull() == 0) {
                    zeroCount++;
                }
            }

            table.addCell(createNumericCell(zeroCount));
            System.out.println("[SummaryReportPdfGenerator] Додано рядок студента (2 модулі): " + student
                    + ", кількість нулів: " + zeroCount);
        }
    }

    private void addZeroSummaryRow(Table table,
                                   List<String> studentFullNames,
                                   List<DisciplineColumn> disciplineColumns,
                                   Map<String, List<Integer>> marksByStudent) {
        System.out.println("[SummaryReportPdfGenerator] Обчислюємо суму нулів по дисциплінах");
        Cell labelCell = new Cell()
                .add(new Paragraph("Кількість 0 по дисциплінах"))
                .setTextAlignment(TextAlignment.LEFT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(1));
        table.addCell(labelCell);

        int totalZeros = 0;
        for (int i = 0; i < disciplineColumns.size(); i++) {
            int zeroPerDiscipline = 0;
            for (String student : studentFullNames) {
                List<Integer> marks = marksByStudent.getOrDefault(student, Collections.emptyList());
                Integer mark = getMark(marks, i);
                if (mark != null && mark == 0) {
                    zeroPerDiscipline++;
                }
            }
            totalZeros += zeroPerDiscipline;
            table.addCell(createNumericCell(zeroPerDiscipline));
            System.out.println("[SummaryReportPdfGenerator] Нулі по дисципліні #" + (i + 1) + ": " + zeroPerDiscipline);
        }

        table.addCell(createNumericCell(totalZeros));
        System.out.println("[SummaryReportPdfGenerator] Загальна кількість нулів: " + totalZeros);
    }

    private void addTwoModuleZeroSummaryRow(Table table,
                                            List<String> studentFullNames,
                                            List<DisciplineColumn> disciplineColumns,
                                            Map<String, List<ModuleMark>> marksByStudent) {
        System.out.println("[SummaryReportPdfGenerator] Обчислюємо суму нулів по дисциплінах (2 модулі)");
        Cell labelCell = new Cell()
                .add(new Paragraph("Кількість 0 по дисциплінах"))
                .setTextAlignment(TextAlignment.LEFT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(1));
        table.addCell(labelCell);

        int totalZeros = 0;
        for (int i = 0; i < disciplineColumns.size(); i++) {
            int zeroPerDiscipline = 0;
            for (String student : studentFullNames) {
                List<ModuleMark> marks = marksByStudent.getOrDefault(student, Collections.emptyList());
                ModuleMark moduleMark = getModuleMark(marks, i);
                if (moduleMark != null && moduleMark.hasAnyMark() && moduleMark.totalOrNull() != null
                        && moduleMark.totalOrNull() == 0) {
                    zeroPerDiscipline++;
                }
            }
            totalZeros += zeroPerDiscipline;
            table.addCell(createEmptyCell());
            table.addCell(createEmptyCell());
            table.addCell(createNumericCell(zeroPerDiscipline));
            System.out.println("[SummaryReportPdfGenerator] Нулі по дисциплінах (2 модулі) #" + (i + 1)
                    + ": " + zeroPerDiscipline);
        }

        table.addCell(createNumericCell(totalZeros));
        System.out.println("[SummaryReportPdfGenerator] Загальна кількість нулів (2 модулі): " + totalZeros);
    }

    private Cell createNumericCell(int value) {
        return new Cell()
                .add(new Paragraph(String.valueOf(value))
                        .setTextAlignment(TextAlignment.CENTER))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(1));
    }

    private Cell createMarkCell(Integer value) {
        if (value == null) {
            return createEmptyCell();
        }
        return createNumericCell(value);
    }

    private Cell createEmptyCell() {
        return new Cell()
                .add(new Paragraph(""))
                .setBorder(new SolidBorder(1));
    }

    private Integer getMark(List<Integer> marks, int index) {
        if (marks == null || index >= marks.size()) {
            return null;
        }
        return marks.get(index);
    }

    private ModuleMark getModuleMark(List<ModuleMark> marks, int index) {
        if (marks == null || index >= marks.size()) {
            return null;
        }
        return marks.get(index);
    }

    public record DisciplineColumn(String title, boolean elective) {
    }

    public record ModuleMark(Integer firstModule, Integer secondModule) {
        public boolean hasAnyMark() {
            return firstModule != null || secondModule != null;
        }

        public Integer totalOrNull() {
            if (!hasAnyMark()) {
                return null;
            }
            int first = firstModule == null ? 0 : firstModule;
            int second = secondModule == null ? 0 : secondModule;
            return first + second;
        }
    }

    public static Div generate(String examiner, Table lastRowTable, boolean numericHeader) throws IOException {
        Div container = new Div();
        if (numericHeader) {
            container.add(NumericHeader.addNumericHeader(lastRowTable, 5));
        } else {
            container.add(lastRowTable);
        }

        container.add(new Paragraph("")
                .setMarginTop(5f)
                .setMarginBottom(5f));
        container.add(Signature.generateSignature(examiner));
        container.setKeepTogether(true);
        return container;
    }

    private static class NumericHeader {

        private static final float NUMERIC_FONT_SIZE = 9f;

        private NumericHeader() {
        }

        // ЗМІНА: public static, + вирівнювання ширин під базову таблицю
        public static Div addNumericHeader(Table baseTable, float marginTop) {
            int totalColumns = baseTable.getNumberOfColumns();
            boolean isTwoModule = (totalColumns - 2) % 3 == 0 && totalColumns > 2;
            float firstColumnWidth = isTwoModule ? TWO_MODULE_FIRST_COLUMN_WIDTH : FIRST_COLUMN_WIDTH;
            float otherColumnWidth = isTwoModule ? TWO_MODULE_OTHER_COLUMN_WIDTH : OTHER_COLUMN_WIDTH;

            // Відтворюємо ті самі ширини, що й у createTableStructure(...)
            float[] widths = new float[totalColumns];
            widths[0] = firstColumnWidth;
            for (int i = 1; i < totalColumns; i++) {
                widths[i] = otherColumnWidth;
            }

            Table numericRow = new Table(widths);
            numericRow.setWidth(UnitValue.createPercentValue(100));
            numericRow.setFixedLayout();

            // перша (ПІБ) — пуста
            numericRow.addCell(createBlankCell());

            // середні — пронумеровані (тільки дисципліни)
            for (int column = 1; column < totalColumns - 1; column++) {
                numericRow.addCell(createNumberCell(column));
            }

            // остання (К-сть 0) — пуста
            numericRow.addCell(createBlankCell());

            Div wrapper = new Div();
            wrapper.setMarginTop(marginTop);
            wrapper.setKeepTogether(true);
            wrapper.add(numericRow);
            wrapper.add(baseTable);
            return wrapper;
        }

        private static Cell createBlankCell() {
            return new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPadding(0);
        }

        private static Cell createNumberCell(int value) {
            return new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPadding(0)
                    .add(new Paragraph(String.valueOf(value))
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontSize(NUMERIC_FONT_SIZE));
        }
    }


    private static class Signature {

        private static final float[] SIGNATURE_TABLE_COLUMNS = {20f, 5f, 20f};
        private static final String SIGNATURE_LINE_HINT = "(підпис)";
        private static final String SIGNATURE_NAME_HINT = "(прізвище, ініціали)";
        private static final float SIGNATURE_FONT_SIZE = 10f;
        private static final float SIGNATURE_HINT_FONT_SIZE = 8f;
        private static final float SIGNATURE_MARGIN_TOP = 10f;

        private Signature() {
        }

        public static Div generateSignature(String examiner) {
            Table table = new Table(UnitValue.createPercentArray(SIGNATURE_TABLE_COLUMNS))
                    .useAllAvailableWidth();

            table.addCell(createSignatureLineCell());
            table.addCell(createSpacerCell());
            table.addCell(createExaminerCell(examiner));

            table.addCell(createHintCell(SIGNATURE_LINE_HINT));
            table.addCell(createSpacerCell());
            table.addCell(createHintCell(SIGNATURE_NAME_HINT));

            Div signatureBlock = new Div();
            signatureBlock.setMarginTop(SIGNATURE_MARGIN_TOP);
            signatureBlock.setKeepTogether(true);
            signatureBlock.add(table);
            return signatureBlock;
        }

        private static Cell createSignatureLineCell() {
            return new Cell()
                    .setBorderTop(Border.NO_BORDER)
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderRight(Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(0.5f))
                    .setPadding(0)
                    .add(new Paragraph("")
                            .setFontSize(SIGNATURE_FONT_SIZE));
        }

        private static Cell createExaminerCell(String examiner) {
            String value = examiner == null ? "" : examiner.trim();
            return new Cell()
                    .setBorderTop(Border.NO_BORDER)
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderRight(Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(0.5f))
                    .setPadding(0)
                    .add(new Paragraph(value)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontSize(SIGNATURE_FONT_SIZE));
        }

        private static Cell createHintCell(String text) {
            return new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPadding(0)
                    .add(new Paragraph(text)
                            .setFontSize(SIGNATURE_HINT_FONT_SIZE)
                            .setTextAlignment(TextAlignment.CENTER));
        }

        private static Cell createSpacerCell() {
            return new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPadding(0)
                    .add(new Paragraph(""));
        }
    }

}
