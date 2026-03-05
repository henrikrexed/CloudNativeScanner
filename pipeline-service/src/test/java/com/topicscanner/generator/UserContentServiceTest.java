package com.topicscanner.generator;

import com.topicscanner.extraction.WebContentExtractor;
import com.topicscanner.llm.LLMService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserContentServiceTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private LLMService llmService;
    @Mock private WebContentExtractor webContentExtractor;

    private UserContentService service;

    @BeforeEach
    void setUp() {
        service = new UserContentService(jdbcTemplate, llmService, webContentExtractor);
    }

    @Test
    void uploadContent_withEmbedding_storesWithVector() {
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
        when(llmService.embed(anyList())).thenReturn(List.of(embedding));
        when(jdbcTemplate.queryForList(contains("INSERT INTO user_content"), eq(Long.class),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of(42L));

        Long id = service.uploadContent("Test Title", "Test content body", "blog_post");

        assertEquals(42L, id);
        verify(llmService).embed(anyList());
    }

    @Test
    void uploadContent_withoutEmbedding_storesWithoutVector() {
        when(llmService.embed(anyList())).thenReturn(List.of());
        when(jdbcTemplate.queryForList(contains("INSERT INTO user_content"), eq(Long.class),
                anyString(), anyString(), anyString()))
                .thenReturn(List.of(43L));

        Long id = service.uploadContent("Title", "Content", "transcript");

        assertEquals(43L, id);
    }

    @Test
    void uploadContent_emptyContent_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.uploadContent("Title", "", "blog_post"));
    }

    @Test
    void uploadContent_nullContent_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.uploadContent("Title", null, "blog_post"));
    }

    @Test
    void importFromUrl_extractionSucceeds_storesContent() {
        when(webContentExtractor.extract("https://example.com/post"))
                .thenReturn(Optional.of("Extracted content from URL"));
        when(llmService.embed(anyList())).thenReturn(List.of());
        when(jdbcTemplate.queryForList(contains("INSERT INTO user_content"), eq(Long.class),
                anyString(), anyString(), anyString()))
                .thenReturn(List.of(44L));

        Long id = service.importFromUrl("https://example.com/post");

        assertEquals(44L, id);
    }

    @Test
    void importFromUrl_extractionFails_throwsIllegalArgument() {
        when(webContentExtractor.extract("https://example.com/paywall"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.importFromUrl("https://example.com/paywall"));
    }

    @Test
    void listContent_returnsAllItems() {
        List<Map<String, Object>> expected = List.of(
                Map.of("id", 1L, "title", "Post 1"),
                Map.of("id", 2L, "title", "Post 2")
        );
        when(jdbcTemplate.queryForList(contains("FROM user_content"))).thenReturn(expected);

        List<Map<String, Object>> result = service.listContent();

        assertEquals(2, result.size());
    }

    @Test
    void deleteContent_existing_returnsTrue() {
        when(jdbcTemplate.update("DELETE FROM user_content WHERE id = ?", 1L)).thenReturn(1);

        assertTrue(service.deleteContent(1L));
    }

    @Test
    void deleteContent_notFound_returnsFalse() {
        when(jdbcTemplate.update("DELETE FROM user_content WHERE id = ?", 999L)).thenReturn(0);

        assertFalse(service.deleteContent(999L));
    }

    @Test
    void findSimilarContent_noEmbedding_returnsEmpty() {
        when(llmService.embed(anyList())).thenReturn(List.of());

        List<Map<String, Object>> result = service.findSimilarContent("some text", 3);

        assertTrue(result.isEmpty());
    }
}
