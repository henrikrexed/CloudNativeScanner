package com.topicscanner.filter.impl;

import com.topicscanner.filter.FilterResult;
import com.topicscanner.filter.TopicContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LanguageFilterTest {

    private final LanguageFilter filter = new LanguageFilter();

    @Test
    void apply_passesEnglishContent() {
        TopicContext ctx = new TopicContext(1L, "Kubernetes Best Practices", "url", "medium",
                "This is a guide on how to deploy Kubernetes in production environments.",
                "Tech", List.of("k8s"), List.of());
        assertTrue(filter.apply(ctx).passed());
    }

    @Test
    void apply_failsNonAsciiContent() {
        TopicContext ctx = new TopicContext(1L, "日本語のタイトル", "url", "medium",
                "これは日本語のコンテンツです。Kubernetesについて説明します。",
                "Tech", List.of("k8s"), List.of());
        assertFalse(filter.apply(ctx).passed());
    }

    @Test
    void apply_passesEmptyContent() {
        TopicContext ctx = new TopicContext(1L, "Title", "url", "medium",
                null, "Tech", List.of(), List.of());
        assertTrue(filter.apply(ctx).passed());
    }

    @Test
    void apply_passesCodeHeavyContent() {
        // Code-heavy content still has enough ASCII and English stop words
        TopicContext ctx = new TopicContext(1L, "Code Example", "url", "medium",
                "Here is the code for a simple HTTP server. This will start the server on port 8080.",
                "Tech", List.of(), List.of());
        assertTrue(filter.apply(ctx).passed());
    }

    @Test
    void getOrder_is30() {
        assertEquals(30, filter.getOrder());
    }
}
