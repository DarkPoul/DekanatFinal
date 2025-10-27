package com.esvar.dekanat.generate.information;

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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GroupListPdfUtil {

    public static byte[] generateGroupListPdf(String groupName,
                                              List<String> students) throws IOException {

        // 1. Почистити імена і відсортувати за українською абеткою
        List<String> cleaned = new ArrayList<>();
        for (String s : students) {
            if (s != null && !s.isBlank()) {
                cleaned.add(s.trim());
            }
        }

        Collator uaCollator = Collator.getInstance(new Locale("uk", "UA"));
        cleaned.sort(uaCollator);

        // 2. Писати PDF у пам'ять
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        pdfDoc.setDefaultPageSize(PageSize.A4);

        Document doc = new Document(pdfDoc);
        doc.setMargins(36, 36, 36, 36); // ~1 см поля

        // 3. Шрифт із resources (src/main/resources/fonts/DejaVuSans.ttf)
        PdfFont font;
        try (InputStream fontStream = GroupListPdfUtil.class
                .getResourceAsStream("/fonts/DejaVuSans.ttf")) {

            if (fontStream == null) {
                throw new IOException("Не знайдено /fonts/DejaVuSans.ttf у resources.");
            }

            byte[] fontBytes = fontStream.readAllBytes();
            font = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H);
        }
        doc.setFont(font);

        // 4. Назва групи по центру
        Paragraph title = new Paragraph(groupName)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4f);

        doc.add(title);

        // 5. Лінія під назвою
        LineSeparator line = new LineSeparator(new SolidLine(1f));
        line.setMarginBottom(16f);
        doc.add(line);

        // 6. Нумерований список студентів
        int idx = 1;
        for (String student : cleaned) {
            Paragraph row = new Paragraph(idx + ". " + student)
                    .setFontSize(12)
                    .setMarginBottom(4f);
            doc.add(row);
            idx++;
        }

        doc.close();
        return baos.toByteArray();
    }
}

