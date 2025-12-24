package com.esvar.dekanat.utilites;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class ContentDispositionUtils {

    private ContentDispositionUtils() {
    }

    public static String buildHeaderValue(String dispositionType, String fileName) {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        String asciiFallback = fileName.replaceAll("[^\\x20-\\x7E]", "_");

        if (asciiFallback.isBlank()) {
            asciiFallback = "file";
        }

        return String.format("%s; filename=\"%s\"; filename*=UTF-8''%s",
                dispositionType,
                asciiFallback,
                encodedFileName);
    }
}
