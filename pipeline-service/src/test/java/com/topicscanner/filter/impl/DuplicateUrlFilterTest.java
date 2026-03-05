package com.topicscanner.filter.impl;

import com.topicscanner.filter.FilterResult;
import com.topicscanner.filter.TopicContext;
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
class DuplicateUrlFilterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void apply_passesForNewUrl() {
        DuplicateUrlFilter filter = new DuplicateUrlFilter(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any()))
                .thenReturn(0);

        TopicContext ctx = new TopicContext(1L, "Title", "https://example.com/new", "medium",
                "Content", "Tech", List.of(), List.of());

        assertTrue(filter.apply(ctx).passed());
    }

    @Test
    void apply_failsForDuplicateUrl() {
        DuplicateUrlFilter filter = new DuplicateUrlFilter(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any()))
                .thenReturn(1);

        TopicContext ctx = new TopicContext(1L, "Title", "https://example.com/existing", "medium",
                "Content", "Tech", List.of(), List.of());

        FilterResult result = filter.apply(ctx);
        assertFalse(result.passed());
        assertTrue(result.reason().contains("Duplicate"));
    }

    @Test
    void getOrder_is10() {
        assertEquals(10, new DuplicateUrlFilter(jdbcTemplate).getOrder());
    }
}
