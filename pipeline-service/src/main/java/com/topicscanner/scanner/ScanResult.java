package com.topicscanner.scanner;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * A single topic discovered by a scanner.
 */
public record ScanResult(
    String title,
    String url,
    String sourceType,
    Map<String, Object> metadata,
    LocalDateTime sourceDate
) {

    public ScanResult {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url must not be blank");
        }
        if (sourceType == null || sourceType.isBlank()) {
            throw new IllegalArgumentException("sourceType must not be blank");
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }
}
