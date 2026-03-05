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

class YouTubeScannerTest {

    private MockWebServer server;
    private YouTubeScanner scanner;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(server.url("/").toString());
        scanner = new YouTubeScanner(builder, "test-api-key");
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void getSourceType_returnsYoutube() {
        assertEquals("youtube", scanner.getSourceType());
    }

    @Test
    void getDisplayName_returnsYouTube() {
        assertEquals("YouTube", scanner.getDisplayName());
    }

    @Test
    void scan_withNoApiKey_returnsEmpty() {
        YouTubeScanner noKeyScanner = new YouTubeScanner(WebClient.builder(), "");
        ScanRequest request = new ScanRequest(List.of("kubernetes"), List.of(), Map.of(), 10);
        List<ScanResult> results = noKeyScanner.scan(request);
        assertTrue(results.isEmpty());
    }

    @Test
    void scan_withEmptyKeywords_returnsEmpty() {
        ScanRequest request = new ScanRequest(List.of(), List.of(), Map.of(), 10);
        List<ScanResult> results = scanner.scan(request);
        assertTrue(results.isEmpty());
    }

    @Test
    void scan_parsesSearchAndStatsResponse() {
        String searchJson = """
                {
                  "items": [
                    {
                      "id": {"videoId": "vid123"},
                      "snippet": {
                        "title": "Kubernetes Tutorial",
                        "channelTitle": "TechChannel",
                        "channelId": "UC123",
                        "publishedAt": "2024-01-15T10:00:00Z"
                      }
                    }
                  ]
                }
                """;
        String statsJson = """
                {
                  "items": [
                    {
                      "id": "vid123",
                      "statistics": {
                        "viewCount": 50000,
                        "likeCount": 1200,
                        "commentCount": 85
                      },
                      "contentDetails": {
                        "duration": "PT15M30S"
                      }
                    }
                  ]
                }
                """;

        server.enqueue(new MockResponse().setBody(searchJson)
                .setHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse().setBody(statsJson)
                .setHeader("Content-Type", "application/json"));

        ScanRequest request = new ScanRequest(List.of("kubernetes"), List.of(), Map.of(), 10);
        List<ScanResult> results = scanner.scan(request);

        assertEquals(1, results.size());
        ScanResult result = results.get(0);
        assertEquals("Kubernetes Tutorial", result.title());
        assertEquals("https://www.youtube.com/watch?v=vid123", result.url());
        assertEquals("youtube", result.sourceType());
        assertEquals("vid123", result.metadata().get("externalId"));
        assertEquals("TechChannel", result.metadata().get("channelTitle"));
        assertEquals(50000L, result.metadata().get("viewCount"));
        assertEquals("PT15M30S", result.metadata().get("duration"));
        assertNotNull(result.sourceDate());
    }

    @Test
    void scan_filtersNegativeKeywords() {
        String searchJson = """
                {
                  "items": [
                    {
                      "id": {"videoId": "vid999"},
                      "snippet": {
                        "title": "HIRING: DevOps Engineers Needed",
                        "channelTitle": "Jobs",
                        "channelId": "UC999",
                        "publishedAt": "2024-01-15T10:00:00Z"
                      }
                    }
                  ]
                }
                """;
        String statsJson = """
                {"items": []}
                """;

        server.enqueue(new MockResponse().setBody(searchJson)
                .setHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse().setBody(statsJson)
                .setHeader("Content-Type", "application/json"));

        ScanRequest request = new ScanRequest(List.of("devops"), List.of("hiring"), Map.of(), 10);
        List<ScanResult> results = scanner.scan(request);
        assertTrue(results.isEmpty());
    }

    @Test
    void scan_handlesApiError() {
        server.enqueue(new MockResponse().setResponseCode(403));

        ScanRequest request = new ScanRequest(List.of("kubernetes"), List.of(), Map.of(), 10);
        List<ScanResult> results = scanner.scan(request);
        assertTrue(results.isEmpty());
    }
}
