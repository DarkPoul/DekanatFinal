package com.esvar.dekanat.card;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generates and streams a simple PDF list of students for a group.
 */
public final class StudentListPdfGenerator {

    private StudentListPdfGenerator() {
    }

    public static void generateAndSend(String title, List<String> students) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            throw new IllegalStateException(
                    "UI.getCurrent() == null. Викликати generateAndSend треба з UI-потоку Vaadin (наприклад у кнопці)."
            );
        }

        try {
            List<String> sortedStudents = prepareStudents(students);
            byte[] pdfBytes = buildPdfBytes(title, sortedStudents);
            String fileName = sanitizeFilename(title) + "-list.pdf";

            StreamResource resource = new StreamResource(
                    fileName,
                    (StreamResourceWriter) (outputStream, session) -> {
                        try (InputStream in = new ByteArrayInputStream(pdfBytes)) {
                            in.transferTo(outputStream);
                        } catch (IOException ioException) {
                            throw new UncheckedIOException(ioException);
                        }
                    }
            );

            resource.setContentType("application/pdf");
            resource.setCacheTime(0);

            StreamRegistration registration = ui.getSession()
                    .getResourceRegistry()
                    .registerResource(resource);

            String resourceUrl = registration.getResourceUri().toString();
            ui.getPage().open(resourceUrl, "_blank");

        } catch (Exception e) {
            throw new RuntimeException("Не вдалося згенерувати або відкрити PDF", e);
        }
    }

    private static List<String> prepareStudents(List<String> students) {
        List<String> cleaned = new ArrayList<>();
        for (String s : students) {
            if (s != null && !s.isBlank()) {
                cleaned.add(s.trim());
            }
        }

        Collator uaCollator = Collator.getInstance(new Locale("uk", "UA"));
        cleaned.sort(uaCollator);

        return cleaned;
    }

    private static byte[] buildPdfBytes(String groupName, List<String> sortedStudents) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        pdfDoc.setDefaultPageSize(PageSize.A4);

        Document doc = new Document(pdfDoc);
        doc.setMargins(36, 36, 36, 36);

        PdfFont font;
        try (InputStream fontStream = CardView.class.getResourceAsStream("/fonts/times.ttf")) {

            if (fontStream == null) {
                throw new IllegalStateException("Не знайдено /fonts/DejaVuSans.ttf у resources.");
            }

            byte[] fontBytes = fontStream.readAllBytes();
            font = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H);
        }
        doc.setFont(font);

        Paragraph titleP = new Paragraph(groupName)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4f);
        doc.add(titleP);

        LineSeparator line = new LineSeparator(new SolidLine(1f));
        line.setMarginBottom(16f);
        doc.add(line);

        int idx = 1;
        for (String student : sortedStudents) {
            Paragraph row = new Paragraph(idx + ". " + student)
                    .setFontSize(12)
                    .setMarginBottom(4f);
            doc.add(row);
            idx++;
        }

        doc.close();
        return baos.toByteArray();
    }

    private static String sanitizeFilename(String in) {
        if (in == null || in.isBlank()) return "group";
        return in.replaceAll("[^a-zA-Z0-9\\u0400-\\u04FF\\-_.]", "_");
    }
}
