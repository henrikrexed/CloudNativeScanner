package com.topicscanner.filter.impl;

import com.topicscanner.filter.FilterResult;
import com.topicscanner.filter.TopicContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContentLengthFilterTest {

    private final ContentLengthFilter filter = new ContentLengthFilter(200);

    @Test
    void apply_passesContentAboveThreshold() {
        String content = "x".repeat(500);
        TopicContext ctx = new TopicContext(1L, "Title", "url", "medium",
                content, "Tech", List.of(), List.of());
        assertTrue(filter.apply(ctx).passed());
    }

    @Test
    void apply_failsContentBelowThreshold() {
        TopicContext ctx = new TopicContext(1L, "Title", "url", "medium",
                "Short", "Tech", List.of(), List.of());
        FilterResult result = filter.apply(ctx);
        assertFalse(result.passed());
        assertTrue(result.reason().contains("too short"));
    }

    @Test
    void apply_failsNullContent() {
        TopicContext ctx = new TopicContext(1L, "Title", "url", "medium",
                null, "Tech", List.of(), List.of());
        assertFalse(filter.apply(ctx).passed());
    }

    @Test
    void apply_exactlyAtThreshold() {
        String content = "x".repeat(200);
        TopicContext ctx = new TopicContext(1L, "Title", "url", "medium",
                content, "Tech", List.of(), List.of());
        assertTrue(filter.apply(ctx).passed());
    }

    @Test
    void getOrder_is40() {
        assertEquals(40, filter.getOrder());
    }
}
