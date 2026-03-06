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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RedditScannerTest {

    private MockWebServer server;
    private RedditScanner scanner;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .defaultHeader("User-Agent", "test")
                .build();
        scanner = new RedditScanner(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void getSourceType_returnsReddit() {
        assertEquals("reddit", scanner.getSourceType());
    }

    @Test
    void getDisplayName_returnsReddit() {
        assertEquals("Reddit", scanner.getDisplayName());
    }

    @Test
    void scan_withEmptyKeywords_returnsEmpty() {
        ScanRequest request = new ScanRequest(List.of(), List.of(), Map.of(), 10);
        List<ScanResult> results = scanner.scan(request);
        assertTrue(results.isEmpty());
    }

    @Test
    void scan_parsesRedditResponse() {
        String json = """
                {
                  "data": {
                    "children": [
                      {
                        "data": {
                          "id": "abc123",
                          "title": "Kubernetes Best Practices",
                          "permalink": "/r/kubernetes/comments/abc123/kubernetes_best_practices/",
                          "score": 42,
                          "num_comments": 10,
                          "upvote_ratio": 0.95,
                          "created_utc": 1700000000
                        }
                      }
                    ]
                  }
                }
                """;
        server.enqueue(new MockResponse().setBody(json)
                .setHeader("Content-Type", "application/json"));

        ScanRequest request = new ScanRequest(
                List.of("kubernetes"), List.of(),
                Map.of("subreddits", List.of("kubernetes")), 10);

        List<ScanResult> results = scanner.scan(request);

        assertEquals(1, results.size());
        ScanResult result = results.get(0);
        assertEquals("Kubernetes Best Practices", result.title());
        assertTrue(result.url().contains("/r/kubernetes/"));
        assertEquals("reddit", result.sourceType());
        assertEquals("abc123", result.metadata().get("externalId"));
        assertEquals(42, result.metadata().get("score"));
    }

    @Test
    void scan_filtersNegativeKeywords() {
        String json = """
                {
                  "data": {
                    "children": [
                      {
                        "data": {
                          "id": "abc123",
                          "title": "Hiring: Kubernetes Engineer",
                          "permalink": "/r/kubernetes/comments/abc123/hiring/",
                          "score": 5,
                          "num_comments": 2,
                          "upvote_ratio": 0.8,
                          "created_utc": 1700000000
                        }
                      }
                    ]
                  }
                }
                """;
        server.enqueue(new MockResponse().setBody(json)
                .setHeader("Content-Type", "application/json"));

        ScanRequest request = new ScanRequest(
                List.of("kubernetes"), List.of("hiring"),
                Map.of("subreddits", List.of("kubernetes")), 10);

        List<ScanResult> results = scanner.scan(request);
        assertTrue(results.isEmpty());
    }

    @Test
    void scan_respectsMaxResults() {
        StringBuilder children = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i > 0) children.append(",");
            children.append("""
                    {
                      "data": {
                        "id": "id%d",
                        "title": "Topic %d",
                        "permalink": "/r/test/comments/id%d/topic/",
                        "score": 1,
                        "num_comments": 0,
                        "upvote_ratio": 0.5,
                        "created_utc": 1700000000
                      }
                    }
                    """.formatted(i, i, i));
        }
        String json = """
                {"data": {"children": [%s]}}
                """.formatted(children);

        server.enqueue(new MockResponse().setBody(json)
                .setHeader("Content-Type", "application/json"));

        ScanRequest request = new ScanRequest(
                List.of("test"), List.of(),
                Map.of("subreddits", List.of("test")), 3);

        List<ScanResult> results = scanner.scan(request);
        assertEquals(3, results.size());
    }

    @Test
    void scan_handlesApiError() {
        server.enqueue(new MockResponse().setResponseCode(500));

        ScanRequest request = new ScanRequest(
                List.of("kubernetes"), List.of(),
                Map.of("subreddits", List.of("test")), 10);

        List<ScanResult> results = scanner.scan(request);
        assertTrue(results.isEmpty());
    }

    @Test
    void extractContent_parsesPostAndComments() {
        String json = """
                [
                  {
                    "data": {
                      "children": [
                        {
                          "data": {
                            "selftext": "This is the post body about Kubernetes."
                          }
                        }
                      ]
                    }
                  },
                  {
                    "data": {
                      "children": [
                        {
                          "data": {
                            "body": "Great post! Here's my experience...",
                            "stickied": false
                          }
                        },
                        {
                          "data": {
                            "body": "Stickied mod comment",
                            "stickied": true
                          }
                        }
                      ]
                    }
                  }
                ]
                """;
        server.enqueue(new MockResponse().setBody(json)
                .setHeader("Content-Type", "application/json"));

        Optional<String> content = scanner.extractContent(server.url("/r/test/comments/abc/").toString());

        assertTrue(content.isPresent());
        assertTrue(content.get().contains("This is the post body about Kubernetes."));
        assertTrue(content.get().contains("Great post!"));
        assertFalse(content.get().contains("Stickied mod comment"));
    }
}
