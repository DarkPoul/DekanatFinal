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

    public static String sanitizeHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Document document = Jsoup.parse(html);
        document.select("script, style, iframe, object, embed").remove();
        Safelist safelist = Safelist.relaxed()
                .addTags("table", "thead", "tbody", "tr", "th", "td", "blockquote")
                .addAttributes(":all", "class", "style")
                .addAttributes("a", "target", "rel")
                .addProtocols("a", "href", "http", "https", "mailto")
                .preserveRelativeLinks(true);
        String cleaned = Jsoup.clean(document.html(), safelist);
        return stripInlinePlaceholders(cleaned);
    }

    public static String stripInlinePlaceholders(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\[image:.*?\\]", "").trim();
    }
}
