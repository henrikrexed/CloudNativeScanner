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
class StyleAnalysisServiceTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private LLMService llmService;

    private StyleAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new StyleAnalysisService(jdbcTemplate, llmService);
    }

    @Test
    void analyzeContent_storesStyleMetadata() {
        String content = "This is a sample blog post about Kubernetes...";
        String styleJson = """
                {"tone":"technical","avg_sentence_length":"medium","vocabulary_complexity":"advanced"}
                """;

        when(jdbcTemplate.queryForList(contains("SELECT content"), eq(1L)))
                .thenReturn(List.of(Map.of("content", content)));
        when(llmService.complete(eq(LLMTaskType.SUMMARIZATION), anyString(), anyString()))
                .thenReturn(new LLMResponse(styleJson, "test-model"));

        String result = service.analyzeContent(1L);

        assertNotNull(result);
        assertTrue(result.contains("tone"));
        verify(jdbcTemplate).update(contains("UPDATE user_content SET writing_style_metadata"), eq(styleJson), eq(1L));
    }

    @Test
    void analyzeContent_contentNotFound_throwsIllegalArgument() {
        when(jdbcTemplate.queryForList(contains("SELECT content"), eq(999L)))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.analyzeContent(999L));
    }

    @Test
    void getCompositeStyleProfile_noStyles_returnsNull() {
        when(jdbcTemplate.queryForList(contains("writing_style_metadata")))
                .thenReturn(List.of());

        assertNull(service.getCompositeStyleProfile());
    }

    @Test
    void getCompositeStyleProfile_singleStyle_returnsSingleProfile() {
        String style = """
                {"tone":"casual"}
                """;
        when(jdbcTemplate.queryForList(contains("writing_style_metadata")))
                .thenReturn(List.of(Map.of("writing_style_metadata", style)));

        String result = service.getCompositeStyleProfile();

        assertEquals(style, result);
        verify(llmService, never()).complete(any(), anyString(), anyString());
    }

    @Test
    void getCompositeStyleProfile_multipleStyles_mergesViaLlm() {
        String style1 = """
                {"tone":"casual"}
                """;
        String style2 = """
                {"tone":"technical"}
                """;
        String merged = """
                {"tone":"mixed"}
                """;

        when(jdbcTemplate.queryForList(contains("writing_style_metadata")))
                .thenReturn(List.of(
                        Map.of("writing_style_metadata", style1),
                        Map.of("writing_style_metadata", style2)));
        when(llmService.complete(eq(LLMTaskType.SUMMARIZATION), anyString(), anyString()))
                .thenReturn(new LLMResponse(merged, "test-model"));

        String result = service.getCompositeStyleProfile();

        assertNotNull(result);
        verify(llmService).complete(eq(LLMTaskType.SUMMARIZATION), anyString(), contains("Sample 1"));
    }
}
