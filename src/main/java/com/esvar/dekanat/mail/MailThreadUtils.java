package com.esvar.dekanat.mail;

import org.springframework.util.StringUtils;

import java.text.Normalizer;

public final class MailThreadUtils {

    private MailThreadUtils() {
    }

    public static String normalizeSubject(String subject) {
        String base = stripPrefixes(subject);
        String normalized = Normalizer.normalize(base, Normalizer.Form.NFKC)
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
        return StringUtils.hasText(normalized) ? normalized : "(no subject)";
    }

    public static String stripPrefixes(String subject) {
        if (!StringUtils.hasText(subject)) {
            return "Без теми";
        }
        String result = subject;
        boolean changed;
        do {
            changed = false;
            String trimmed = result.stripLeading();
            if (trimmed.matches("(?i)^(re|fw|fwd|aw|пере?д|відповідь)\\s*[:\\]\\)\\-]*\\s*.*")) {
                result = trimmed.replaceFirst("(?i)^(re|fw|fwd|aw|пере?д|відповідь)\\s*[:\\]\\)\\-]*\\s*", "");
                changed = true;
            }
        } while (changed);
        return result.strip();
    }

    public static String buildThreadKey(String peerEmail, String subject) {
        String peer = StringUtils.hasText(peerEmail) ? peerEmail.trim().toLowerCase() : "(unknown)";
        String normalizedSubject = normalizeSubject(subject);
        return peer + "|" + normalizedSubject;
    }
}
