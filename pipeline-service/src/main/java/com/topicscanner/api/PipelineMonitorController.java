package com.topicscanner.api;

import com.topicscanner.queue.ScanOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Pipeline Monitor REST API — job queue status, errors, scan history.
 * Serves the Pipeline Monitor view in the Next.js UI.
 */
@RestController
@RequestMapping("/api/pipeline")
@CrossOrigin(origins = "*")
public class PipelineMonitorController {

    private final JdbcTemplate jdbcTemplate;
    private final ScanOrchestrator scanOrchestrator;

    public PipelineMonitorController(JdbcTemplate jdbcTemplate, ScanOrchestrator scanOrchestrator) {
        this.jdbcTemplate = jdbcTemplate;
        this.scanOrchestrator = scanOrchestrator;
    }

    /**
     * GET /api/pipeline/status — job queue overview (counts by stage x status).
     */
    @GetMapping("/status")
    public Map<String, Object> getPipelineStatus() {
        List<Map<String, Object>> jobCounts = jdbcTemplate.queryForList(
                """
                SELECT stage, status, COUNT(*) AS count
                FROM pipeline_jobs
                GROUP BY stage, status
                ORDER BY stage, status
                """);

        Long totalPending = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pipeline_jobs WHERE status = 'PENDING'",
                Long.class);

        Long totalProcessing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pipeline_jobs WHERE status = 'PROCESSING'",
                Long.class);

        Long totalCompleted = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pipeline_jobs WHERE status = 'COMPLETED'",
                Long.class);

        Long totalFailed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pipeline_jobs WHERE status = 'FAILED'",
                Long.class);

        return Map.of(
                "jobCounts", jobCounts,
                "summary", Map.of(
                        "pending", totalPending != null ? totalPending : 0,
                        "processing", totalProcessing != null ? totalProcessing : 0,
                        "completed", totalCompleted != null ? totalCompleted : 0,
                        "failed", totalFailed != null ? totalFailed : 0
                )
        );
    }

    /**
     * GET /api/pipeline/errors — recent failed jobs.
     */
    @GetMapping("/errors")
    public List<Map<String, Object>> getRecentErrors(
            @RequestParam(defaultValue = "20") int limit) {
        return jdbcTemplate.queryForList(
                """
                SELECT pj.id, pj.stage, pj.topic_id, pj.error_message,
                       pj.retry_count, pj.created_at,
                       t.title AS topic_title, t.url AS topic_url
                FROM pipeline_jobs pj
                LEFT JOIN topics t ON pj.topic_id = t.id
                WHERE pj.status = 'FAILED'
                ORDER BY pj.created_at DESC
                LIMIT ?
                """,
                Math.min(limit, 100));
    }

    /**
     * GET /api/pipeline/scan-history — recent scan runs.
     */
    @GetMapping("/scan-history")
    public List<Map<String, Object>> getScanHistory(
            @RequestParam(defaultValue = "20") int limit) {
        return jdbcTemplate.queryForList(
                """
                SELECT sh.id, sh.source, sh.topics_found, sh.topics_new,
                       sh.errors, sh.error_details,
                       sh.started_at, sh.completed_at,
                       c.name AS category_name
                FROM scan_history sh
                LEFT JOIN categories c ON sh.category_id = c.id
                ORDER BY sh.started_at DESC
                LIMIT ?
                """,
                Math.min(limit, 100));
    }

    /**
     * POST /api/pipeline/scan — trigger an immediate scan.
     */
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> triggerScan() {
        try {
            scanOrchestrator.scheduledScan();
            return ResponseEntity.ok(Map.of("triggered", true, "message", "Scan started"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("triggered", false, "error", e.getMessage()));
        }
    }

    /**
     * GET /api/pipeline/topics-by-stage — topic counts by pipeline stage.
     */
    @GetMapping("/topics-by-stage")
    public List<Map<String, Object>> getTopicsByStage() {
        return jdbcTemplate.queryForList(
                """
                SELECT pipeline_stage, COUNT(*) AS count
                FROM topics
                GROUP BY pipeline_stage
                ORDER BY pipeline_stage
                """);
    }
}
