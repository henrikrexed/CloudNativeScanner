package com.topicscanner.scanner;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the SourceScanner interface contract using a minimal stub implementation.
 * This serves as the basis for the ScannerTestHarness pattern.
 */
class SourceScannerContractTest {

    /**
     * Minimal scanner stub for contract verification.
     */
    static class StubScanner implements SourceScanner {

        @Override
        public String getSourceType() {
            return "stub";
        }

        @Override
        public String getDisplayName() {
            return "Stub Scanner";
        }

        @Override
        public List<ScanResult> scan(ScanRequest request) {
            return request.keywords().stream()
                    .map(kw -> new ScanResult(
                            "Result for " + kw,
                            "https://stub.example.com/" + kw,
                            getSourceType(),
                            Map.of("keyword", kw),
                            LocalDateTime.now()
                    ))
                    .toList();
        }
    }

    /**
     * Scanner that also implements custom extraction.
     */
    static class ExtractingScanner implements SourceScanner {

        @Override
        public String getSourceType() {
            return "extractor";
        }

        @Override
        public String getDisplayName() {
            return "Extracting Scanner";
        }

        @Override
        public List<ScanResult> scan(ScanRequest request) {
            return List.of();
        }

        @Override
        public Optional<String> extractContent(String url) {
            return Optional.of("Extracted content from " + url);
        }
    }

    private final StubScanner stubScanner = new StubScanner();
    private final ExtractingScanner extractingScanner = new ExtractingScanner();

    @Test
    void sourceType_isStable() {
        assertEquals("stub", stubScanner.getSourceType());
        assertEquals("stub", stubScanner.getSourceType()); // idempotent
    }

    @Test
    void displayName_isNonEmpty() {
        assertFalse(stubScanner.getDisplayName().isBlank());
    }

    @Test
    void scan_returnsResultsMatchingKeywords() {
        var request = new ScanRequest(
                List.of("kubernetes", "docker"),
                List.of(),
                Map.of(),
                25
        );

        List<ScanResult> results = stubScanner.scan(request);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> r.sourceType().equals("stub")));
    }

    @Test
    void scan_emptyKeywords_returnsEmptyList() {
        var request = new ScanRequest(List.of(), List.of(), Map.of(), 25);
        List<ScanResult> results = stubScanner.scan(request);
        assertTrue(results.isEmpty());
    }

    @Test
    void extractContent_defaultReturnsEmpty() {
        Optional<String> content = stubScanner.extractContent("https://example.com");
        assertTrue(content.isEmpty());
    }

    @Test
    void extractContent_overridden_returnsContent() {
        Optional<String> content = extractingScanner.extractContent("https://example.com/article");
        assertTrue(content.isPresent());
        assertTrue(content.get().contains("https://example.com/article"));
    }

    @Test
    void scanResults_haveCorrectSourceType() {
        var request = new ScanRequest(List.of("test"), List.of(), Map.of(), 10);
        List<ScanResult> results = stubScanner.scan(request);

        for (ScanResult result : results) {
            assertEquals(stubScanner.getSourceType(), result.sourceType());
        }
    }
}
