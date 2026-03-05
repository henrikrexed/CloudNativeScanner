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

class HashnodeScannerTest {

    private MockWebServer server;
    private HashnodeScanner scanner;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(server.url("/").toString());
        scanner = new HashnodeScanner(builder, "test-token");
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void getSourceType_returnsHashnode() {
        assertEquals("hashnode", scanner.getSourceType());
    }

    @Test
    void getDisplayName_returnsHashnode() {
        assertEquals("Hashnode", scanner.getDisplayName());
    }

    @Test
    void scan_withEmptyKeywords_returnsEmpty() {
        ScanRequest request = new ScanRequest(List.of(), List.of(), Map.of(), 10);
        List<ScanResult> results = scanner.scan(request);
        assertTrue(results.isEmpty());
    }

    @Test
    void scan_parsesGraphQLResponse() {
        String json = """
                {
                  "data": {
                    "feed": {
                      "edges": [
                        {
                          "node": {
                            "id": "hn123",
                            "title": "Kubernetes in Production",
                            "brief": "A guide to running K8s in production...",
                            "slug": "kubernetes-in-production",
                            "url": "https://blog.example.com/kubernetes-in-production",
                            "publishedAt": "2024-01-20T12:00:00Z",
                            "reactionCount": 45,
                            "replyCount": 8,
                            "publication": {
                              "url": "https://blog.example.com"
                            }
                          }
                        }
                      ]
                    }
                  }
                }
                """;
        server.enqueue(new MockResponse().setBody(json)
                .setHeader("Content-Type", "application/json"));

        ScanRequest request = new ScanRequest(List.of("kubernetes"), List.of(), Map.of(), 10);
        List<ScanResult> results = scanner.scan(request);

        assertEquals(1, results.size());
        ScanResult result = results.get(0);
        assertEquals("Kubernetes in Production", result.title());
        assertEquals("https://blog.example.com/kubernetes-in-production", result.url());
        assertEquals("hashnode", result.sourceType());
        assertEquals("hn123", result.metadata().get("externalId"));
        assertEquals(45, result.metadata().get("reactionCount"));
    }

    @Test
    void scan_filtersNegativeKeywords() {
        String json = """
                {
                  "data": {
                    "feed": {
                      "edges": [
                        {
                          "node": {
                            "id": "hn999",
                            "title": "Hiring: We need Kubernetes experts",
                            "brief": "Job posting",
                            "slug": "hiring-k8s",
                            "url": "https://blog.example.com/hiring-k8s",
                            "publishedAt": "2024-01-20T12:00:00Z",
                            "reactionCount": 1,
                            "replyCount": 0,
                            "publication": {"url": "https://blog.example.com"}
                          }
                        }
                      ]
                    }
                  }
                }
                """;
        server.enqueue(new MockResponse().setBody(json)
                .setHeader("Content-Type", "application/json"));

        ScanRequest request = new ScanRequest(List.of("kubernetes"), List.of("hiring"), Map.of(), 10);
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
    void scan_worksWithoutApiToken() throws IOException {
        MockWebServer noTokenServer = new MockWebServer();
        noTokenServer.start();

        HashnodeScanner noTokenScanner = new HashnodeScanner(
                WebClient.builder().baseUrl(noTokenServer.url("/").toString()), "");

        String json = """
                {
                  "data": {
                    "feed": {
                      "edges": [
                        {
                          "node": {
                            "id": "hn1",
                            "title": "Test Post",
                            "brief": "Brief",
                            "slug": "test",
                            "url": "https://test.com/test",
                            "publishedAt": "2024-01-01T00:00:00Z",
                            "reactionCount": 0,
                            "replyCount": 0,
                            "publication": {"url": "https://test.com"}
                          }
                        }
                      ]
                    }
                  }
                }
                """;
        noTokenServer.enqueue(new MockResponse().setBody(json)
                .setHeader("Content-Type", "application/json"));

        ScanRequest request = new ScanRequest(List.of("test"), List.of(), Map.of(), 10);
        List<ScanResult> results = noTokenScanner.scan(request);
        assertEquals(1, results.size());

        noTokenServer.shutdown();
    }
}
