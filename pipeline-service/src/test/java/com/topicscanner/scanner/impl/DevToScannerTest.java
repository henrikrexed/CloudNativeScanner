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

class DevToScannerTest {

    private MockWebServer server;
    private DevToScanner scanner;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(server.url("/").toString());
        scanner = new DevToScanner(builder);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void getSourceType_returnsDevto() {
        assertEquals("devto", scanner.getSourceType());
    }

    @Test
    void getDisplayName_returnsDevTo() {
        assertEquals("Dev.to", scanner.getDisplayName());
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
                [
                  {
                    "id": 54321,
                    "title": "Getting Started with Go",
                    "url": "https://dev.to/author/getting-started-with-go-abc",
                    "published_at": "2024-01-10T08:30:00Z",
                    "positive_reactions_count": 120,
                    "comments_count": 15,
                    "reading_time_minutes": 8,
                    "tag_list": ["go", "programming"],
                    "user": {
                      "username": "godev"
                    }
                  }
                ]
                """;
        server.enqueue(new MockResponse().setBody(json)
                .setHeader("Content-Type", "application/json"));

        ScanRequest request = new ScanRequest(List.of("go"), List.of(), Map.of(), 10);
        List<ScanResult> results = scanner.scan(request);

        assertEquals(1, results.size());
        ScanResult result = results.get(0);
        assertEquals("Getting Started with Go", result.title());
        assertEquals("https://dev.to/author/getting-started-with-go-abc", result.url());
        assertEquals("devto", result.sourceType());
        assertEquals("54321", result.metadata().get("externalId"));
        assertEquals("godev", result.metadata().get("author"));
        assertEquals(120, result.metadata().get("positiveReactionsCount"));
        assertEquals(8, result.metadata().get("readingTimeMinutes"));
    }

    @Test
    void scan_filtersNegativeKeywords() {
        String json = """
                [
                  {
                    "id": 1,
                    "title": "We are hiring Go developers!",
                    "url": "https://dev.to/company/hiring-go-devs",
                    "published_at": "2024-01-10T08:30:00Z",
                    "positive_reactions_count": 5,
                    "comments_count": 0,
                    "reading_time_minutes": 2,
                    "tag_list": [],
                    "user": {"username": "company"}
                  }
                ]
                """;
        server.enqueue(new MockResponse().setBody(json)
                .setHeader("Content-Type", "application/json"));

        ScanRequest request = new ScanRequest(List.of("go"), List.of("hiring"), Map.of(), 10);
        List<ScanResult> results = scanner.scan(request);
        assertTrue(results.isEmpty());
    }

    @Test
    void scan_handlesApiError() {
        server.enqueue(new MockResponse().setResponseCode(429));

        ScanRequest request = new ScanRequest(List.of("go"), List.of(), Map.of(), 10);
        List<ScanResult> results = scanner.scan(request);
        assertTrue(results.isEmpty());
    }
}
