package com.esvar.dekanat.mail;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class MailTextExtractor {

    private MailTextExtractor() {
    }

    public static String toPlainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Document document = Jsoup.parse(html);
        document.select("style, script").remove();
        return document.text();
    }

    public static String sanitizeSnippet(String content, int maxLength) {
        String text = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.min(maxLength, text.length()));
    }
}
