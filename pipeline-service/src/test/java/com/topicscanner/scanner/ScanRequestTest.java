package com.topicscanner.scanner;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScanRequestTest {

    @Test
    void validRequest() {
        var request = new ScanRequest(
                List.of("kubernetes", "docker"),
                List.of("war"),
                Map.of("subreddit", "devops"),
                50
        );

        assertEquals(2, request.keywords().size());
        assertEquals(1, request.negativeKeywords().size());
        assertEquals("devops", request.scannerConfig().get("subreddit"));
        assertEquals(50, request.maxResults());
    }

    @Test
    void nullLists_defaultToEmpty() {
        var request = new ScanRequest(null, null, null, 10);

        assertNotNull(request.keywords());
        assertTrue(request.keywords().isEmpty());
        assertNotNull(request.negativeKeywords());
        assertTrue(request.negativeKeywords().isEmpty());
        assertNotNull(request.scannerConfig());
        assertTrue(request.scannerConfig().isEmpty());
    }

    @Test
    void zeroMaxResults_defaultsTo25() {
        var request = new ScanRequest(List.of("k8s"), List.of(), Map.of(), 0);
        assertEquals(25, request.maxResults());
    }

    @Test
    void negativeMaxResults_defaultsTo25() {
        var request = new ScanRequest(List.of("k8s"), List.of(), Map.of(), -5);
        assertEquals(25, request.maxResults());
    }
}
