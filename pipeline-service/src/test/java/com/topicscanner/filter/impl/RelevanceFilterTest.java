package com.topicscanner.filter.impl;

import com.topicscanner.filter.FilterResult;
import com.topicscanner.filter.TopicContext;
import com.topicscanner.llm.LLMException;
import com.topicscanner.llm.LLMResponse;
import com.topicscanner.llm.LLMService;
import com.topicscanner.llm.LLMTaskType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelevanceFilterTest {

    @Mock
    private LLMService llmService;

    @Test
    void apply_passesHighRelevanceScore() {
        RelevanceFilter filter = new RelevanceFilter(llmService, 0.7);
        when(llmService.complete(eq(LLMTaskType.RELEVANCE), any(), any()))
                .thenReturn(new LLMResponse("0.85", "test-model"));

        TopicContext ctx = new TopicContext(1L, "K8s Guide", "url", "medium",
                "Content about Kubernetes", "Tech", List.of("kubernetes"), List.of());

        FilterResult result = filter.apply(ctx);
        assertTrue(result.passed());
        assertEquals(0.85, result.score(), 0.01);
    }

    @Test
    void apply_failsLowRelevanceScore() {
        RelevanceFilter filter = new RelevanceFilter(llmService, 0.7);
        when(llmService.complete(eq(LLMTaskType.RELEVANCE), any(), any()))
                .thenReturn(new LLMResponse("0.3", "test-model"));

        TopicContext ctx = new TopicContext(1L, "Recipe Blog", "url", "medium",
                "How to make pasta", "Tech", List.of("kubernetes"), List.of());

        FilterResult result = filter.apply(ctx);
        assertFalse(result.passed());
        assertTrue(result.reason().contains("0.30"));
    }

    @Test
    void apply_passesOnLLMException() {
        RelevanceFilter filter = new RelevanceFilter(llmService, 0.7);
        when(llmService.complete(eq(LLMTaskType.RELEVANCE), any(), any()))
                .thenThrow(new LLMException("Provider unavailable"));

        TopicContext ctx = new TopicContext(1L, "Title", "url", "medium",
                "Content", "Tech", List.of("k8s"), List.of());

        FilterResult result = filter.apply(ctx);
        assertTrue(result.passed()); // Don't block on LLM failure
    }

    @Test
    void apply_storesScoreInContext() {
        RelevanceFilter filter = new RelevanceFilter(llmService, 0.7);
        when(llmService.complete(eq(LLMTaskType.RELEVANCE), any(), any()))
                .thenReturn(new LLMResponse("0.9", "test-model"));

        TopicContext ctx = new TopicContext(1L, "Title", "url", "medium",
                "Content", "Tech", List.of("k8s"), List.of());
        filter.apply(ctx);

        assertEquals(0.9, (Double) ctx.getAttribute("relevanceScore"), 0.01);
    }

    @Test
    void getOrder_is50() {
        assertEquals(50, new RelevanceFilter(llmService, 0.7).getOrder());
    }
}
