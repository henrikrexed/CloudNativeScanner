package com.topicscanner.filter.impl;

import com.topicscanner.filter.FilterResult;
import com.topicscanner.filter.TopicContext;
import com.topicscanner.llm.LLMException;
import com.topicscanner.llm.LLMService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingDedupFilterTest {

    @Mock
    private LLMService llmService;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void apply_passesForUniqueContent() {
        EmbeddingDedupFilter filter = new EmbeddingDedupFilter(llmService, jdbcTemplate, 0.95);

        when(llmService.embed(anyList())).thenReturn(List.of(new float[]{0.1f, 0.2f, 0.3f}));
        when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), any(), any()))
                .thenReturn(0.5); // Low similarity

        TopicContext ctx = new TopicContext(1L, "Unique Topic", "url", "medium",
                "Unique content", "Tech", List.of(), List.of());

        FilterResult result = filter.apply(ctx);
        assertTrue(result.passed());
        assertNotNull(ctx.getAttribute("embeddingVector"));
    }

    @Test
    void apply_failsForDuplicateContent() {
        EmbeddingDedupFilter filter = new EmbeddingDedupFilter(llmService, jdbcTemplate, 0.95);

        when(llmService.embed(anyList())).thenReturn(List.of(new float[]{0.1f, 0.2f, 0.3f}));
        when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), any(), any()))
                .thenReturn(0.98); // High similarity = duplicate

        TopicContext ctx = new TopicContext(1L, "Duplicate Topic", "url", "medium",
                "Same old content", "Tech", List.of(), List.of());

        FilterResult result = filter.apply(ctx);
        assertFalse(result.passed());
        assertTrue(result.reason().contains("0.980"));
    }

    @Test
    void apply_passesOnEmbeddingFailure() {
        EmbeddingDedupFilter filter = new EmbeddingDedupFilter(llmService, jdbcTemplate, 0.95);

        when(llmService.embed(anyList())).thenThrow(new LLMException("test", "Embedding failed"));

        TopicContext ctx = new TopicContext(1L, "Title", "url", "medium",
                "Content", "Tech", List.of(), List.of());

        FilterResult result = filter.apply(ctx);
        assertTrue(result.passed()); // Don't block on embedding failure
    }

    @Test
    void apply_passesWhenNoExistingTopics() {
        EmbeddingDedupFilter filter = new EmbeddingDedupFilter(llmService, jdbcTemplate, 0.95);

        when(llmService.embed(anyList())).thenReturn(List.of(new float[]{0.1f, 0.2f}));
        when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), any(), any()))
                .thenReturn(null); // No existing topics with embeddings

        TopicContext ctx = new TopicContext(1L, "Title", "url", "medium",
                "Content", "Tech", List.of(), List.of());

        assertTrue(filter.apply(ctx).passed());
    }

    @Test
    void getOrder_is70() {
        assertEquals(70, new EmbeddingDedupFilter(llmService, jdbcTemplate, 0.95).getOrder());
    }
}
