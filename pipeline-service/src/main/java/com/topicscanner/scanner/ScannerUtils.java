package com.topicscanner.scanner;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Shared utility methods for scanner implementations.
 */
public final class ScannerUtils {

    private ScannerUtils() {}

    /**
     * Check if text matches any negative keyword (case-insensitive).
     */
    public static boolean matchesNegativeKeywords(String text, List<String> negativeKeywords) {
        if (text == null || negativeKeywords == null || negativeKeywords.isEmpty()) {
            return false;
        }
        String lower = text.toLowerCase();
        return negativeKeywords.stream()
                .anyMatch(kw -> lower.contains(kw.toLowerCase()));
    }

    /**
     * Parse an ISO date-time string to LocalDateTime.
     */
    public static LocalDateTime parseIsoDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
