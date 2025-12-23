package com.esvar.dekanat.mail.v2.service;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public final class SubjectNormalizer {

    private static final Pattern RE_PREFIX = Pattern.compile("(?i)^(?:re(\\[[0-9]+])?:\\s*)+");
    private static final Pattern FWD_PREFIX = Pattern.compile("(?i)^(?:fwd?:\\s*)+");

    private SubjectNormalizer() {
    }

    public static String normalize(String subject) {
        if (!StringUtils.hasText(subject)) {
            return subject;
        }
        String cleaned = subject.trim();
        cleaned = RE_PREFIX.matcher(cleaned).replaceFirst("");
        cleaned = FWD_PREFIX.matcher(cleaned).replaceFirst("");
        return cleaned.trim();
    }

    public static String buildReplySubject(String subject) {
        String normalized = normalize(subject);
        String base = StringUtils.hasText(normalized) ? normalized : " ";
        return "Re: " + base;
    }
}
