package com.esvar.dekanat.mail;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility methods for removing quoted history from emails.
 */
public final class MailQuotedStripper {

    private static final Pattern QUOTE_HEADER = Pattern.compile(
            "(?im)^\\s*(" +
                    "On\\s.+wrote:|" +
                    ".+\\sпише:|" +
                    ".+\\sписав\\(ла\\):|" +
                    "-----\\s*Original Message\\s*-----|" +
                    "-----\\s*Forwarded message\\s*-----|" +
                    "From:|" +
                    "Від:|" +
                    "От:|" +
                    "Sent:|" +
                    "Дата:|" +
                    "To:|" +
                    "Кому:|" +
                    "Subject:|" +
                    "Тема:" +
                    ")\\s*$"
    );

    private static final Set<String> HTML_QUOTE_SELECTORS = new HashSet<>(Arrays.asList(
            "blockquote",
            ".gmail_quote",
            ".gmail_extra",
            ".yahoo_quoted",
            ".moz-cite-prefix",
            "[class*=moz-cite-prefix]"
    ));

    private MailQuotedStripper() {
    }

    public static String stripQuotedPlain(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.stripLeading();
            if (trimmed.startsWith(">")) {
                break;
            }
            if (trimmed.startsWith("-- ")) {
                break;
            }
            if (QUOTE_HEADER.matcher(trimmed).find()) {
                break;
            }
            result.append(line).append("\n");
        }
        return result.toString().replaceFirst("(?s)\\n+$", "").trim();
    }

    public static String stripQuotedHtml(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        Document document = Jsoup.parse(html);
        String selectors = HTML_QUOTE_SELECTORS.stream().collect(Collectors.joining(","));
        document.select(selectors).remove();
        return MailTextExtractor.sanitizeHtml(document.body().html());
    }
}
