package com.topicscanner.generator;

import com.topicscanner.llm.LLMResponse;
import com.topicscanner.llm.LLMService;
import com.topicscanner.llm.LLMTaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentGenerationServiceTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private LLMService llmService;
    @Mock private StyleAnalysisService styleAnalysisService;
    @Mock private UserContentService userContentService;

    private ContentGenerationService service;

    @BeforeEach
    void setUp() {
        service = new ContentGenerationService(
                jdbcTemplate, llmService, styleAnalysisService, userContentService);
    }

    @Test
    void generate_withTopicId_producesContent() {
        // Topic data
        when(jdbcTemplate.queryForList(contains("FROM topics WHERE id"), eq(1L)))
                .thenReturn(List.of(Map.of(
                        "title", "Kubernetes 101",
                        "extracted_content", "Kubernetes content...",
                        "summary", "A guide to K8s",
                        "url", "https://example.com/k8s",
                        "source_date", "2024-01-01")))
                .thenReturn(List.of(Map.of("group_id", "grp-1")));

        // No cluster topics
        when(jdbcTemplate.queryForList(contains("WHERE group_id"), anyString(), anyLong()))
                .thenReturn(List.of());

        // Style profile
        when(styleAnalysisService.getCompositeStyleProfile()).thenReturn(null);

        // RAG context
        when(userContentService.findSimilarContent(anyString(), eq(3))).thenReturn(List.of());

        // LLM generation
        when(llmService.complete(eq(LLMTaskType.GENERATION), anyString(), anyString()))
                .thenReturn(new LLMResponse("# Generated Blog Post\n\nContent here...", "gpt-4o"));

        // Insert generated content
        when(jdbcTemplate.queryForList(contains("INSERT INTO generated_content"), eq(Long.class),
                any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(10L));

        Long id = service.generate(1L, null, "blog_post");

        assertEquals(10L, id);
        verify(llmService).complete(eq(LLMTaskType.GENERATION), anyString(), anyString());
    }

    @Test
    void generate_noTopicOrGroup_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.generate(null, null, "blog_post"));
    }

    @Test
    void generate_topicNotFound_throwsIllegalArgument() {
        when(jdbcTemplate.queryForList(contains("FROM topics WHERE id"), eq(999L)))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> service.generate(999L, null, "blog_post"));
    }

    @Test
    void regenerate_withFeedback_producesNewVersion() {
        // Original content
        when(jdbcTemplate.queryForList(contains("FROM generated_content"), eq(10L)))
                .thenReturn(List.of(Map.of(
                        "topic_id", 1L,
                        "group_id", "grp-1",
                        "output_format", "blog_post",
                        "generated_text", "Original draft...",
                        "model_used", "gpt-4o")));

        // Count existing regenerations
        when(jdbcTemplate.queryForList(contains("COUNT(*)"), eq(Long.class), any(), any()))
                .thenReturn(List.of(1L));

        // LLM regeneration
        when(llmService.complete(eq(LLMTaskType.GENERATION), anyString(), anyString()))
                .thenReturn(new LLMResponse("Revised draft with more technical detail...", "gpt-4o"));

        // Insert new version
        when(jdbcTemplate.queryForList(contains("INSERT INTO generated_content"), eq(Long.class),
                any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(11L));

        Long newId = service.regenerate(10L, "make it more technical");

        assertEquals(11L, newId);
        verify(jdbcTemplate).update(contains("UPDATE generated_content SET feedback"), eq("make it more technical"), eq(10L));
    }

    @Test
    void regenerate_notFound_throwsIllegalArgument() {
        when(jdbcTemplate.queryForList(contains("FROM generated_content"), eq(999L)))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> service.regenerate(999L, "feedback"));
    }

    @Test
    void regenerate_maxReached_throwsIllegalState() {
        when(jdbcTemplate.queryForList(contains("FROM generated_content"), eq(10L)))
                .thenReturn(List.of(Map.of(
                        "topic_id", 1L,
                        "group_id", "grp-1",
                        "output_format", "blog_post",
                        "generated_text", "Draft",
                        "model_used", "gpt-4o")));

        when(jdbcTemplate.queryForList(contains("COUNT(*)"), eq(Long.class), any(), any()))
                .thenReturn(List.of(5L));

        assertThrows(IllegalStateException.class,
                () -> service.regenerate(10L, "more changes"));
    }

    @Test
    void exportAsMarkdown_existing_returnsText() {
        when(jdbcTemplate.queryForList(contains("FROM generated_content gc"), eq(10L)))
                .thenReturn(List.of(Map.of("generated_text", "# Blog Post\n\nContent")));

        String md = service.exportAsMarkdown(10L);

        assertEquals("# Blog Post\n\nContent", md);
    }

    @Test
    void exportAsMarkdown_notFound_returnsNull() {
        when(jdbcTemplate.queryForList(contains("FROM generated_content gc"), eq(999L)))
                .thenReturn(List.of());

        assertNull(service.exportAsMarkdown(999L));
    }

    @Test
    void generate_youtubeScript_usesCorrectPrompt() {
        when(jdbcTemplate.queryForList(contains("FROM topics WHERE id"), eq(1L)))
                .thenReturn(List.of(Map.of(
                        "title", "Topic",
                        "extracted_content", "Content",
                        "summary", "Summary",
                        "url", "https://example.com",
                        "source_date", "2024-01-01")))
                .thenReturn(List.of(Map.of()));

        when(styleAnalysisService.getCompositeStyleProfile()).thenReturn(null);
        when(userContentService.findSimilarContent(anyString(), eq(3))).thenReturn(List.of());
        when(llmService.complete(eq(LLMTaskType.GENERATION), contains("YouTube"), anyString()))
                .thenReturn(new LLMResponse("Script content", "gpt-4o"));
        when(jdbcTemplate.queryForList(contains("INSERT INTO generated_content"), eq(Long.class),
                any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(12L));

        Long id = service.generate(1L, null, "youtube_script");

        assertEquals(12L, id);
        verify(llmService).complete(eq(LLMTaskType.GENERATION), contains("YouTube"), anyString());
    }
}
