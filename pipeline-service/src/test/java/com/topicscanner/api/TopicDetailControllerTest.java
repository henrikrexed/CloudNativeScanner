package com.topicscanner.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicDetailControllerTest {

    @Mock private JdbcTemplate jdbcTemplate;

    private TopicDetailController controller;

    @BeforeEach
    void setUp() {
        controller = new TopicDetailController(jdbcTemplate);
    }

    @Test
    void getTopicDetail_existing_returnsTopicWithRelated() {
        when(jdbcTemplate.queryForList(contains("FROM topics t"), eq(1L)))
                .thenReturn(List.of(new java.util.HashMap<>(Map.of(
                        "id", 1L,
                        "title", "Kubernetes Guide",
                        "group_id", "grp-abc",
                        "source_type", "Medium"
                ))));

        when(jdbcTemplate.queryForList(contains("WHERE t.group_id"), eq("grp-abc"), eq(1L)))
                .thenReturn(List.of(Map.of("id", 2L, "title", "Related K8s Post")));

        var response = controller.getTopicDetail(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Kubernetes Guide", response.getBody().get("title"));
        assertInstanceOf(List.class, response.getBody().get("relatedTopics"));
    }

    @Test
    void getTopicDetail_notFound_returns404() {
        when(jdbcTemplate.queryForList(contains("FROM topics t"), eq(999L)))
                .thenReturn(List.of());

        var response = controller.getTopicDetail(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getTopicDetail_noGroupId_returnsEmptyRelated() {
        when(jdbcTemplate.queryForList(contains("FROM topics t"), eq(1L)))
                .thenReturn(List.of(new java.util.HashMap<String, Object>() {{
                    put("id", 1L);
                    put("title", "Solo Topic");
                    put("group_id", null);
                    put("source_type", "Reddit");
                }}));

        var response = controller.getTopicDetail(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(), response.getBody().get("relatedTopics"));
    }
}
