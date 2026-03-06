package com.topicscanner.extraction;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StackOverflowExtractorTest {

    private MockWebServer server;
    private StackOverflowExtractor extractor;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .build();
        extractor = new StackOverflowExtractor(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void extractQuestionId_fromStandardUrl() {
        assertEquals("12345",
                extractor.extractQuestionId("https://stackoverflow.com/questions/12345/how-to-do-x"));
    }

    @Test
    void extractQuestionId_fromShortUrl() {
        assertEquals("67890",
                extractor.extractQuestionId("https://stackoverflow.com/questions/67890"));
    }

    @Test
    void extractQuestionId_returnsNullForInvalidUrl() {
        assertNull(extractor.extractQuestionId("https://stackoverflow.com/tags/java"));
    }

    @Test
    void extract_returnsFormattedQAndA() {
        String questionJson = """
                {
                  "items": [
                    {
                      "title": "How to deploy Kubernetes?",
                      "body": "<p>I need help deploying K8s on bare metal.</p>",
                      "score": 42,
                      "view_count": 5000
                    }
                  ]
                }
                """;
        String answersJson = """
                {
                  "items": [
                    {
                      "body": "<p>Use kubeadm for bare metal deployments.</p>",
                      "score": 85,
                      "is_accepted": true
                    },
                    {
                      "body": "<p>Consider k3s for lighter setups.</p>",
                      "score": 30,
                      "is_accepted": false
                    }
                  ]
                }
                """;
        server.enqueue(new MockResponse().setBody(questionJson)
                .setHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse().setBody(answersJson)
                .setHeader("Content-Type", "application/json"));

        Optional<String> content = extractor.extract(
                "https://stackoverflow.com/questions/12345/how-to-deploy-kubernetes");

        assertTrue(content.isPresent());
        String text = content.get();
        assertTrue(text.contains("## Question"));
        assertTrue(text.contains("How to deploy Kubernetes?"));
        assertTrue(text.contains("I need help deploying K8s on bare metal."));
        assertTrue(text.contains("Accepted Answer"));
        assertTrue(text.contains("kubeadm"));
        assertTrue(text.contains("k3s"));
        assertTrue(text.contains("Score: 85"));
    }

    @Test
    void extract_handlesNoAnswers() {
        String questionJson = """
                {
                  "items": [
                    {
                      "title": "Unanswered question?",
                      "body": "<p>This question has no answers yet.</p>",
                      "score": 1,
                      "view_count": 50
                    }
                  ]
                }
                """;
        String answersJson = """
                {"items": []}
                """;
        server.enqueue(new MockResponse().setBody(questionJson)
                .setHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse().setBody(answersJson)
                .setHeader("Content-Type", "application/json"));

        Optional<String> content = extractor.extract(
                "https://stackoverflow.com/questions/99999/unanswered");

        assertTrue(content.isPresent());
        assertTrue(content.get().contains("## Question"));
        assertFalse(content.get().contains("Accepted Answer"));
    }

    @Test
    void extract_handlesApiError() {
        server.enqueue(new MockResponse().setResponseCode(500));

        Optional<String> content = extractor.extract(
                "https://stackoverflow.com/questions/12345/test");

        assertTrue(content.isEmpty());
    }

    @Test
    void extract_returnsEmptyForInvalidUrl() {
        Optional<String> content = extractor.extract("https://stackoverflow.com/tags/java");
        assertTrue(content.isEmpty());
    }
}
