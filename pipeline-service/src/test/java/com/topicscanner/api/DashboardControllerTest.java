package com.topicscanner.api;

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
class DashboardControllerTest {

    @Mock private JdbcTemplate jdbcTemplate;

    private DashboardController controller;

    @BeforeEach
    void setUp() {
        controller = new DashboardController(jdbcTemplate);
    }

    @Test
    void listTopics_defaultParams_returnsPaginatedResults() {
        when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Long.class), any(Object[].class)))
                .thenReturn(42L);
        when(jdbcTemplate.queryForList(contains("FROM topics"), any(Object[].class)))
                .thenReturn(List.of(
                        Map.of("id", 1L, "title", "Topic 1"),
                        Map.of("id", 2L, "title", "Topic 2")
                ));

        Map<String, Object> result = controller.listTopics(0, 20, null, null, null, null, null);

        assertEquals(42L, result.get("total"));
        assertEquals(0, result.get("page"));
        assertEquals(20, result.get("size"));
        assertInstanceOf(List.class, result.get("topics"));
    }

    @Test
    void listTopics_withCategoryFilter_addsWhereClause() {
        when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Long.class), any(Object[].class)))
                .thenReturn(10L);
        when(jdbcTemplate.queryForList(contains("FROM topics"), any(Object[].class)))
                .thenReturn(List.of());

        Map<String, Object> result = controller.listTopics(0, 20, 5L, null, null, null, null);

        assertEquals(10L, result.get("total"));
    }

    @Test
    void getStats_returnsAllStatsFields() {
        when(jdbcTemplate.queryForObject(contains("pipeline_stage = 'analyzed'"), eq(Long.class)))
                .thenReturn(100L);
        when(jdbcTemplate.queryForObject(contains("7 days"), eq(Long.class)))
                .thenReturn(15L);
        when(jdbcTemplate.queryForObject(contains("PENDING"), eq(Long.class)))
                .thenReturn(3L);
        when(jdbcTemplate.queryForObject(contains("FAILED"), eq(Long.class)))
                .thenReturn(1L);
        when(jdbcTemplate.queryForList(contains("categories")))
                .thenReturn(List.of(Map.of("name", "Cloud Native", "topic_count", 50)));
        when(jdbcTemplate.queryForList(contains("sources")))
                .thenReturn(List.of(Map.of("name", "Medium", "topic_count", 30)));

        Map<String, Object> stats = controller.getStats();

        assertEquals(100L, stats.get("totalTopics"));
        assertEquals(15L, stats.get("topicsThisWeek"));
        assertEquals(3L, stats.get("pendingJobs"));
        assertEquals(1L, stats.get("failedJobs"));
        assertNotNull(stats.get("byCategory"));
        assertNotNull(stats.get("bySource"));
    }
}
