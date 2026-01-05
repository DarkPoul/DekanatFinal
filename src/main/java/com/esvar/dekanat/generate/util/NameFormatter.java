package com.esvar.dekanat.generate.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility methods for formatting personal names in a consistent way across PDF/Docx generators.
 */
public final class NameFormatter {

    private NameFormatter() {
    }

    /**
     * Formats a name as "SURNAME I.P." (surname uppercased, initials capitalized).
     */
    public static String formatSurnameWithInitials(String surname, String name, String patronymic) {
        String normalizedSurname = safe(surname).toUpperCase();
        String initials = Stream.of(name, patronymic)
                .map(NameFormatter::initial)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(""));

        return Stream.of(normalizedSurname, initials)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(" ")).trim();
    }

    /**
     * Splits a free-form full name by whitespace and formats it with initials.
     */
    public static String formatFullName(String fullName) {
        String cleaned = safe(fullName).trim();
        if (cleaned.isEmpty()) {
            return "";
        }

        String[] parts = cleaned.split("\\s+");
        boolean hasDottedParts = Arrays.stream(parts, 1, parts.length)
                .anyMatch(part -> part.contains("."));
        if (hasDottedParts) {
            String surname = parts[0].toUpperCase();
            String rest = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length)).trim();
            return Stream.of(surname, rest)
                    .filter(part -> !part.isBlank())
                    .collect(Collectors.joining(" ")).trim();
        }

        String surname = parts.length > 0 ? parts[0] : "";
        String name = parts.length > 1 ? parts[1] : "";
        String patronymic = parts.length > 2 ? parts[2] : "";
        return formatSurnameWithInitials(surname, name, patronymic);
    }

    private static String initial(String value) {
        String trimmed = safe(value).trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        int firstCodePoint = trimmed.codePointAt(0);
        return new String(Character.toChars(Character.toUpperCase(firstCodePoint))) + ".";
    }

    private static String safe(String value) {
        return Objects.toString(value, "");
    }
}
