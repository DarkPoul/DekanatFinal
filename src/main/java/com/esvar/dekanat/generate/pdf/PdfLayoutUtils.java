package com.esvar.dekanat.generate.pdf;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.OverflowPropertyValue;
import com.itextpdf.layout.properties.Property;
import com.itextpdf.layout.properties.TextAlignment;


/**
 * Shared helpers for building PDF table cells with predictable layout behaviour.
 */
public final class PdfLayoutUtils {

    private static final float DEFAULT_BORDER_WIDTH = 0.5f;
    private static final float DEFAULT_PADDING = 4f;
    private static final float DEFAULT_FONT_SIZE = 10f;
    private static final float MIN_FONT_SIZE = 8f;

    private PdfLayoutUtils() {
    }

    public static Cell studentCell(String text, PdfFont font, TextAlignment alignment) {
        return studentCell(text, font, alignment, 1, 1);
    }

    public static Cell studentCell(String text,
                                   PdfFont font,
                                   TextAlignment alignment,
                                   int rowSpan,
                                   int colSpan) {
        Paragraph paragraph = createClampedParagraph(text, font, alignment, DEFAULT_FONT_SIZE);
        return new Cell(rowSpan, colSpan)
                .add(paragraph)
                .setBorder(new SolidBorder(DEFAULT_BORDER_WIDTH))
                .setPadding(DEFAULT_PADDING)
                .setKeepTogether(true);
    }

    public static Paragraph createClampedParagraph(String text,
                                                   PdfFont font,
                                                   TextAlignment alignment,
                                                   float baseFontSize) {
        String safe = safeText(text);
        float adjustedSize = adjustFontSize(safe, baseFontSize);

        Paragraph paragraph = new Paragraph(safe)
                .setFont(font)
                .setFontSize(adjustedSize)
                .setTextAlignment(alignment)
                .setMargin(0)
                .setMultipliedLeading(1f);

        paragraph.setProperty(Property.OVERFLOW_X, OverflowPropertyValue.HIDDEN);
        paragraph.setProperty(Property.OVERFLOW_Y, OverflowPropertyValue.HIDDEN);
        paragraph.setKeepTogether(true);
        paragraph.setFixedLeading(adjustedSize * 1.2f);


        return paragraph;
    }

    public static String safeText(String value) {
        return value == null ? "" : value;
    }

    public static String resolveLabel(String value, String defaultLabel) {
        String label = safeText(value);
        return label.isBlank() ? defaultLabel : label;
    }

    private static float adjustFontSize(String value, float baseFontSize) {
        if (value.length() > 60) {
            return Math.max(MIN_FONT_SIZE, baseFontSize - 2f);
        }
        if (value.length() > 40) {
            return Math.max(MIN_FONT_SIZE, baseFontSize - 1f);
        }
        return baseFontSize;
    }
}
