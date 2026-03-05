package com.topicscanner.api;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Topic Detail REST API — full topic info, content, and related topics.
 * Serves the Topic Detail view in the Next.js UI.
 */
@RestController
@RequestMapping("/api/topics")
@CrossOrigin(origins = "*")
public class TopicDetailController {

    private final JdbcTemplate jdbcTemplate;

    public TopicDetailController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * GET /api/topics/{id} — full topic details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTopicDetail(@PathVariable Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT t.id, t.title, t.url, t.content, t.extracted_content, t.summary,
                       t.author, t.source_date, t.created_at,
                       t.quality_score, t.relevance_score, t.relevance_reasoning,
                       t.pipeline_stage, t.extraction_status,
                       t.tags, t.group_id, t.language,
                       t.interaction_count, t.view_count, t.score, t.engagement_score,
                       s.name AS source_type,
                       c.name AS category_name
                FROM topics t
                JOIN sources s ON t.source_id = s.id
                LEFT JOIN categories c ON t.category_id = c.id
                WHERE t.id = ?
                """,
                id);

        if (rows.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> topic = new java.util.HashMap<>(rows.get(0));

        // Load related topics from same cluster
        String groupId = (String) topic.get("group_id");
        if (groupId != null && !groupId.isBlank()) {
            List<Map<String, Object>> related = jdbcTemplate.queryForList(
                    """
                    SELECT t.id, t.title, t.url, t.summary, t.quality_score,
                           s.name AS source_type, t.source_date
                    FROM topics t
                    JOIN sources s ON t.source_id = s.id
                    WHERE t.group_id = ? AND t.id != ?
                    ORDER BY t.quality_score DESC NULLS LAST
                    LIMIT 10
                    """,
                    groupId, id);
            topic.put("relatedTopics", related);
        } else {
            topic.put("relatedTopics", List.of());
        }

        return ResponseEntity.ok(topic);
    }
}
