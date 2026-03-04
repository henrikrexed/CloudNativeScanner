package com.topicscanner.scanner;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScanResultTest {

    @Test
    void validScanResult() {
        var result = new ScanResult(
                "eBPF in Kubernetes",
                "https://example.com/ebpf",
                "reddit",
                Map.of("subreddit", "kubernetes"),
                LocalDateTime.of(2026, 3, 1, 10, 0)
        );

        assertEquals("eBPF in Kubernetes", result.title());
        assertEquals("https://example.com/ebpf", result.url());
        assertEquals("reddit", result.sourceType());
        assertEquals("kubernetes", result.metadata().get("subreddit"));
        assertNotNull(result.sourceDate());
    }

    @Test
    void nullMetadata_defaultsToEmptyMap() {
        var result = new ScanResult("Title", "https://example.com", "reddit", null, null);
        assertNotNull(result.metadata());
        assertTrue(result.metadata().isEmpty());
    }

    @Test
    void blankTitle_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScanResult("", "https://example.com", "reddit", null, null));
    }

    @Test
    void nullTitle_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScanResult(null, "https://example.com", "reddit", null, null));
    }

    @Test
    void blankUrl_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScanResult("Title", " ", "reddit", null, null));
    }

    @Test
    void blankSourceType_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScanResult("Title", "https://example.com", "", null, null));
    }
}
