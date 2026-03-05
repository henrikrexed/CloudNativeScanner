package com.topicscanner.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Dashboard REST API — topic feed, stats, and filtering.
 * Serves the Dashboard view in the Next.js UI.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final JdbcTemplate jdbcTemplate;

    public DashboardController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * GET /api/topics — paginated topic feed with optional filters.
     */
    @GetMapping("/topics")
    public Map<String, Object> listTopics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Double minQuality) {

        StringBuilder where = new StringBuilder("WHERE t.pipeline_stage = 'analyzed' AND t.is_rejected = false");
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (categoryId != null) {
            where.append(" AND t.category_id = ?");
            params.add(categoryId);
        }
        if (sourceType != null && !sourceType.isBlank()) {
            where.append(" AND s.name = ?");
            params.add(sourceType);
        }
        if (dateFrom != null && !dateFrom.isBlank()) {
            where.append(" AND t.created_at >= ?::timestamp");
            params.add(dateFrom);
        }
        if (dateTo != null && !dateTo.isBlank()) {
            where.append(" AND t.created_at <= ?::timestamp");
            params.add(dateTo);
        }
        if (minQuality != null) {
            where.append(" AND t.quality_score >= ?");
            params.add(minQuality);
        }

        // Count
        String countSql = "SELECT COUNT(*) FROM topics t JOIN sources s ON t.source_id = s.id " + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        // Page query
        params.add(size);
        params.add(page * size);
        String dataSql = """
                SELECT t.id, t.title, t.url, t.summary,
                       t.quality_score, t.relevance_score,
                       t.source_date, t.created_at,
                       s.name AS source_type,
                       c.name AS category_name,
                       t.group_id
                FROM topics t
                JOIN sources s ON t.source_id = s.id
                LEFT JOIN categories c ON t.category_id = c.id
                """ + where + """
                ORDER BY t.created_at DESC
                LIMIT ? OFFSET ?
                """;

        List<Map<String, Object>> topics = jdbcTemplate.queryForList(dataSql, params.toArray());

        return Map.of(
                "topics", topics,
                "total", total != null ? total : 0,
                "page", page,
                "size", size
        );
    }

    /**
     * GET /api/stats — dashboard statistics.
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Long totalTopics = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM topics WHERE pipeline_stage = 'analyzed' AND is_rejected = false",
                Long.class);

        Long topicsThisWeek = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM topics WHERE created_at > NOW() - INTERVAL '7 days' AND pipeline_stage = 'analyzed'",
                Long.class);

        Long pendingJobs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pipeline_jobs WHERE status = 'PENDING'",
                Long.class);

        Long failedJobs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pipeline_jobs WHERE status = 'FAILED'",
                Long.class);

        List<Map<String, Object>> byCategory = jdbcTemplate.queryForList(
                """
                SELECT c.name, COUNT(t.id) AS topic_count
                FROM categories c
                LEFT JOIN topics t ON t.category_id = c.id AND t.pipeline_stage = 'analyzed'
                WHERE c.enabled = true
                GROUP BY c.name
                ORDER BY topic_count DESC
                """);

        List<Map<String, Object>> bySource = jdbcTemplate.queryForList(
                """
                SELECT s.name, COUNT(t.id) AS topic_count
                FROM sources s
                LEFT JOIN topics t ON t.source_id = s.id AND t.pipeline_stage = 'analyzed'
                GROUP BY s.name
                ORDER BY topic_count DESC
                """);

        return Map.of(
                "totalTopics", totalTopics != null ? totalTopics : 0,
                "topicsThisWeek", topicsThisWeek != null ? topicsThisWeek : 0,
                "pendingJobs", pendingJobs != null ? pendingJobs : 0,
                "failedJobs", failedJobs != null ? failedJobs : 0,
                "byCategory", byCategory,
                "bySource", bySource
        );
    }
}
