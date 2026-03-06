package com.topicscanner.filter.impl;

import com.topicscanner.filter.FilterResult;
import com.topicscanner.filter.TopicContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QualityFilterTest {

    private final QualityFilter filter = new QualityFilter(0.5);

    @Test
    void apply_passesHighQualityContent() {
        String content = """
                ## Introduction

                This is a comprehensive guide to deploying Kubernetes in production.
                It covers all the essential best practices and common pitfalls.

                ## Architecture

                The architecture consists of multiple components working together.
                Each component has a specific role in the overall system.

                ## Configuration

                Here is the configuration for the deployment:

                ```yaml
                apiVersion: apps/v1
                kind: Deployment
                ```

                ## Best Practices

                - Always set resource limits
                - Use namespaces for isolation
                - Enable RBAC for security
                - Monitor with Prometheus

                ## Conclusion

                Following these practices will ensure a stable production environment.
                """;
        TopicContext ctx = new TopicContext(1L, "Title", "url", "medium",
                content, "Tech", List.of(), List.of());
        FilterResult result = filter.apply(ctx);
        assertTrue(result.passed());
        assertTrue(result.score() >= 0.5);
    }

    @Test
    void apply_failsLowQualityContent() {
        TopicContext ctx = new TopicContext(1L, "Title", "url", "medium",
                "Just a single short paragraph with no structure at all.",
                "Tech", List.of(), List.of());
        // This may or may not pass depending on threshold; with 0.5 it should fail
        FilterResult result = filter.apply(ctx);
        assertTrue(result.score() < 0.5);
    }

    @Test
    void apply_failsNullContent() {
        TopicContext ctx = new TopicContext(1L, "Title", "url", "medium",
                null, "Tech", List.of(), List.of());
        assertFalse(filter.apply(ctx).passed());
    }

    @Test
    void calculateQualityScore_longContentScoresHigher() {
        double shortScore = filter.calculateQualityScore("Short content");
        double longScore = filter.calculateQualityScore("x".repeat(5001));
        assertTrue(longScore > shortScore);
    }

    @Test
    void calculateQualityScore_structuredContentScoresHigher() {
        double plain = filter.calculateQualityScore("Plain text without any structure.");
        double structured = filter.calculateQualityScore("""
                ## Heading 1

                First paragraph content.

                ## Heading 2

                Second paragraph content.

                - Item 1
                - Item 2
                - Item 3
                """);
        assertTrue(structured > plain);
    }

    @Test
    void apply_storesQualityScoreInContext() {
        String content = "x".repeat(3000) + "\n\n## Heading\n\nParagraph.\n\n## Another\n\nMore text.";
        TopicContext ctx = new TopicContext(1L, "Title", "url", "medium",
                content, "Tech", List.of(), List.of());
        filter.apply(ctx);
        assertNotNull(ctx.getAttribute("qualityScore"));
    }

    @Test
    void getOrder_is45() {
        assertEquals(45, filter.getOrder());
    }
}
