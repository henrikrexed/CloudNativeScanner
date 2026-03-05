package com.topicscanner.filter.impl;

import com.topicscanner.filter.FilterResult;
import com.topicscanner.filter.TopicContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NegativeKeywordFilterTest {

    private final NegativeKeywordFilter filter = new NegativeKeywordFilter();

    @Test
    void apply_passesWithNoNegativeKeywords() {
        TopicContext ctx = new TopicContext(1L, "Kubernetes Guide", "url", "medium",
                "Content about K8s", "Tech", List.of("k8s"), List.of());
        assertTrue(filter.apply(ctx).passed());
    }

    @Test
    void apply_failsOnTitleMatch() {
        TopicContext ctx = new TopicContext(1L, "Hiring: K8s Engineers", "url", "medium",
                "Job posting content", "Tech", List.of("k8s"), List.of("hiring"));
        FilterResult result = filter.apply(ctx);
        assertFalse(result.passed());
        assertTrue(result.reason().contains("hiring"));
    }

    @Test
    void apply_failsOnContentMatch() {
        TopicContext ctx = new TopicContext(1L, "Good Title", "url", "medium",
                "This is a sponsored post about K8s", "Tech", List.of("k8s"), List.of("sponsored"));
        assertFalse(filter.apply(ctx).passed());
    }

    @Test
    void apply_caseInsensitive() {
        TopicContext ctx = new TopicContext(1L, "HIRING DevOps", "url", "medium",
                "Content", "Tech", List.of("devops"), List.of("hiring"));
        assertFalse(filter.apply(ctx).passed());
    }

    @Test
    void apply_passesWhenNoMatch() {
        TopicContext ctx = new TopicContext(1L, "Kubernetes Best Practices", "url", "medium",
                "Production guide for K8s", "Tech", List.of("k8s"), List.of("hiring", "sponsored"));
        assertTrue(filter.apply(ctx).passed());
    }
}
