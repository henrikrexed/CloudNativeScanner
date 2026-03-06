package com.topicscanner.scanner.impl;

import com.topicscanner.scanner.ScanRequest;
import com.topicscanner.scanner.ScanResult;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StackOverflowScannerTest {

    private MockWebServer server;
    private StackOverflowScanner scanner;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .defaultHeader("User-Agent", "test")
                .build();
        scanner = new StackOverflowScanner(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void getSourceType_returnsStackoverflow() {
        assertEquals("stackoverflow", scanner.getSourceType());
    }

    @Test
    void getDisplayName_returnsStackOverflow() {
        assertEquals("Stack Overflow", scanner.getDisplayName());
    }

    @Test
    void scan_withEmptyKeywords_returnsEmpty() {
        ScanRequest request = new ScanRequest(List.of(), List.of(), Map.of(), 10);
        List<ScanResult> results = scanner.scan(request);
        assertTrue(results.isEmpty());
    }

    @Test
    void scan_parsesResponse() {
        String json = """
                {
                  "items": [
                    {
                      "question_id": 12345,
                      "title": "How to deploy on Kubernetes?",
                      "link": "https://stackoverflow.com/questions/12345",
                      "score": 25,
                      "answer_count": 3,
                      "view_count": 1500,
                      "is_answered": true,
                      "tags": ["kubernetes", "docker"],
                      "creation_date": 1700000000
                    }
                  ],
                  "has_more": false
                }
                """;
        server.enqueue(new MockResponse().setBody(json)
                .setHeader("Content-Type", "application/json"));

        ScanRequest request = new ScanRequest(List.of("kubernetes"), List.of(), Map.of(), 10);
        List<ScanResult> results = scanner.scan(request);

        assertEquals(1, results.size());
        ScanResult result = results.get(0);
        assertEquals("How to deploy on Kubernetes?", result.title());
        assertEquals("https://stackoverflow.com/questions/12345", result.url());
        assertEquals("stackoverflow", result.sourceType());
        assertEquals("12345", result.metadata().get("externalId"));
        assertEquals(25, result.metadata().get("score"));
        assertEquals(true, result.metadata().get("isAnswered"));
    }

    @Test
    void scan_filtersNegativeKeywords() {
        String json = """
                {
                  "items": [
                    {
                      "question_id": 99,
                      "title": "Hiring a DevOps engineer",
                      "link": "https://stackoverflow.com/questions/99",
                      "score": 1,
                      "answer_count": 0,
                      "view_count": 10,
                      "is_answered": false,
                      "tags": [],
                      "creation_date": 1700000000
                    }
                  ],
                  "has_more": false
                }
                """;
        server.enqueue(new MockResponse().setBody(json)
                .setHeader("Content-Type", "application/json"));

        ScanRequest request = new ScanRequest(List.of("devops"), List.of("hiring"), Map.of(), 10);
        List<ScanResult> results = scanner.scan(request);
        assertTrue(results.isEmpty());
    }

    @Test
    void scan_handlesApiError() {
        server.enqueue(new MockResponse().setResponseCode(500));

        ScanRequest request = new ScanRequest(List.of("kubernetes"), List.of(), Map.of(), 10);
        List<ScanResult> results = scanner.scan(request);
        assertTrue(results.isEmpty());
    }

    @Test
    void scan_respectsMaxResults() {
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i > 0) items.append(",");
            items.append("""
                    {
                      "question_id": %d,
                      "title": "Question %d",
                      "link": "https://stackoverflow.com/questions/%d",
                      "score": 1,
                      "answer_count": 0,
                      "view_count": 10,
                      "is_answered": false,
                      "tags": [],
                      "creation_date": 1700000000
                    }
                    """.formatted(i, i, i));
        }
        String json = """
                {"items": [%s], "has_more": false}
                """.formatted(items);

        server.enqueue(new MockResponse().setBody(json)
                .setHeader("Content-Type", "application/json"));

        ScanRequest request = new ScanRequest(List.of("test"), List.of(), Map.of(), 2);
        List<ScanResult> results = scanner.scan(request);
        assertEquals(2, results.size());
    }
}
