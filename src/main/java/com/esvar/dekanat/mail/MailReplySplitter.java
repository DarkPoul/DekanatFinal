package com.esvar.dekanat.mail;

import java.util.regex.Pattern;

public final class MailReplySplitter {

    private MailReplySplitter() {}

    public record SplitResult(String reply, String quoted) {}

    // Типові маркери початку цитування (Gmail/Outlook/uk/ru)
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

    public static SplitResult split(String text) {
        if (text == null || text.isBlank()) return new SplitResult("", "");

        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);

        int cut = -1;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 1) Явний заголовок цитування
            if (QUOTE_HEADER.matcher(line).find()) {
                cut = i;
                break;
            }

            // 2) Якщо почались quoted-рядки
            if (line.stripLeading().startsWith(">")) {
                cut = i;
                break;
            }
        }

        if (cut == -1) {
            return new SplitResult(normalized.trim(), "");
        }

        String reply = join(lines, 0, cut).trim();
        String quoted = join(lines, cut, lines.length).trim();

        // Приберемо зайві пусті рядки зверху/знизу quoted
        quoted = quoted.replaceFirst("(?s)^\\s+", "").replaceFirst("(?s)\\s+$", "");

        return new SplitResult(reply, quoted);
    }

    private static String join(String[] lines, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            sb.append(lines[i]);
            if (i < to - 1) sb.append('\n');
        }
        return sb.toString();
    }
}
