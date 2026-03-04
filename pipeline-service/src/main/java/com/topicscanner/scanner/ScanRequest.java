package com.topicscanner.scanner;

import java.util.List;
import java.util.Map;

/**
 * Input to a scanner's scan() method.
 * Contains the keywords to search for and scanner-specific configuration.
 */
public record ScanRequest(
    List<String> keywords,
    List<String> negativeKeywords,
    Map<String, Object> scannerConfig,
    int maxResults
) {

    public ScanRequest {
        if (keywords == null) {
            keywords = List.of();
        }
        if (negativeKeywords == null) {
            negativeKeywords = List.of();
        }
        if (scannerConfig == null) {
            scannerConfig = Map.of();
        }
        if (maxResults <= 0) {
            maxResults = 25;
        }
    }
}
