package com.esvar.dekanat.generate.pdf;

import com.esvar.dekanat.document.DocumentException;
import com.esvar.dekanat.document.PdfGenerator;
import com.esvar.dekanat.generate.BasicControlPdfData;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class BasicControlPdfGenerator implements PdfGenerator {

    public static final String NAME = "basic-control-placeholder";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Path generatePdf(Object data) {
        if (!(data instanceof BasicControlPdfData controlData)) {
            throw new DocumentException("Expected BasicControlPdfData for placeholder generation");
        }

        String controlName = Optional.ofNullable(controlData.controlTypeName())
                .filter(name -> !name.isBlank())
                .orElse("Невідомий вид контролю");

        try {
            Path outputPath = resolveOutputPath(controlName);
            Files.createDirectories(outputPath.getParent());

            try (PdfWriter writer = new PdfWriter(outputPath.toFile());
                 PdfDocument pdfDocument = new PdfDocument(writer);
                 Document document = new Document(pdfDocument, PageSize.A4)) {

                document.add(new Paragraph(controlName)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(18)
                        );

                document.add(new Paragraph("Відомість буде допрацьована пізніше")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(12));
            }

            return outputPath;
        } catch (IOException ex) {
            throw new DocumentException("Failed to generate placeholder PDF", ex);
        }
    }

    private Path resolveOutputPath(String controlName) {
        String safeName = controlName.replaceAll("\\s+", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileName = "control-" + safeName + "-" + timestamp + ".pdf";
        return Paths.get("uploads").resolve(fileName);
    }
}
