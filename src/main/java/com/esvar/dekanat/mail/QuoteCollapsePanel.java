package com.esvar.dekanat.mail;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public class QuoteCollapsePanel extends Details {

    private static final Pattern ON_WROTE = Pattern.compile("(?i)^on .+ wrote:");
    private static final Pattern FORWARDED = Pattern.compile("(?i)forwarded message");
    private static final Pattern ORIGINAL_MESSAGE = Pattern.compile("(?i)^[-]{2,}\\s*original message", Pattern.MULTILINE);

    public QuoteCollapsePanel(String quoteHtml) {
        setSummaryText("Показати цитату");
        Div content = new Div();
        content.addClassName("quote-content");
        content.getElement().setProperty("innerHTML", quoteHtml);
        setContent(content);
        addClassName("quote-collapse");
    }

    public static QuoteExtraction extract(String body) {
        if (!StringUtils.hasText(body)) {
            return new QuoteExtraction("", "");
        }
        String[] lines = body.split("\\r?\\n");
        StringBuilder main = new StringBuilder();
        StringBuilder quote = new StringBuilder();
        boolean inQuote = false;
        for (String line : lines) {
            String trimmed = line.stripLeading();
            if (isQuoteStart(trimmed)) {
                inQuote = true;
            }
            if (inQuote) {
                quote.append(line).append("\n");
            } else {
                main.append(line).append("\n");
            }
        }
        return new QuoteExtraction(main.toString().trim(), quote.toString().trim());
    }

    private static boolean isQuoteStart(String line) {
        if (!StringUtils.hasText(line)) {
            return false;
        }
        return line.startsWith(">") || ON_WROTE.matcher(line).find() || FORWARDED.matcher(line).find()
                || ORIGINAL_MESSAGE.matcher(line).find();
    }

    public record QuoteExtraction(String main, String quote) {
        public boolean hasQuote() {
            return StringUtils.hasText(quote);
        }
    }
}
