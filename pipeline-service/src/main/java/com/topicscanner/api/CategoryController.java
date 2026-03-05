package com.topicscanner.api;

import com.topicscanner.scanner.ScannerRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Category CRUD REST API + scanner listing.
 * Serves the Categories view in the Next.js UI.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CategoryController {

    private final JdbcTemplate jdbcTemplate;
    private final ScannerRegistry scannerRegistry;

    public CategoryController(JdbcTemplate jdbcTemplate, ScannerRegistry scannerRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.scannerRegistry = scannerRegistry;
    }

    /**
     * GET /api/categories — list all categories with topic counts.
     */
    @GetMapping("/categories")
    public List<Map<String, Object>> listCategories() {
        return jdbcTemplate.queryForList(
                """
                SELECT c.id, c.name, c.description, c.keywords, c.negative_keywords,
                       c.sources, c.scan_frequency, c.enabled,
                       COUNT(t.id) FILTER (WHERE t.pipeline_stage = 'analyzed') AS topic_count,
                       MAX(sh.completed_at) AS last_scan_time,
                       c.created_at, c.updated_at
                FROM categories c
                LEFT JOIN topics t ON t.category_id = c.id
                LEFT JOIN scan_history sh ON sh.category_id = c.id
                GROUP BY c.id
                ORDER BY c.name
                """);
    }

    /**
     * GET /api/categories/{id} — single category details.
     */
    @GetMapping("/categories/{id}")
    public ResponseEntity<Map<String, Object>> getCategory(@PathVariable Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT c.*, COUNT(t.id) FILTER (WHERE t.pipeline_stage = 'analyzed') AS topic_count
                FROM categories c
                LEFT JOIN topics t ON t.category_id = c.id
                WHERE c.id = ?
                GROUP BY c.id
                """,
                id);
        if (rows.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rows.get(0));
    }

    /**
     * POST /api/categories — create a new category.
     */
    @PostMapping("/categories")
    public ResponseEntity<Map<String, Object>> createCategory(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "name is required"));
        }

        List<Long> ids = jdbcTemplate.queryForList(
                """
                INSERT INTO categories (name, description, keywords, negative_keywords, sources, scan_frequency, enabled)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                name,
                body.getOrDefault("description", ""),
                body.getOrDefault("keywords", ""),
                body.getOrDefault("negativeKeywords", ""),
                body.getOrDefault("sources", ""),
                body.getOrDefault("scanFrequency", "daily"),
                body.getOrDefault("enabled", true));

        Long id = ids.get(0);
        return ResponseEntity.status(201).body(Map.of("id", id, "name", name));
    }

    /**
     * PUT /api/categories/{id} — update a category.
     */
    @PutMapping("/categories/{id}")
    public ResponseEntity<Map<String, Object>> updateCategory(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {

        int updated = jdbcTemplate.update(
                """
                UPDATE categories
                SET name = COALESCE(?, name),
                    description = COALESCE(?, description),
                    keywords = COALESCE(?, keywords),
                    negative_keywords = COALESCE(?, negative_keywords),
                    sources = COALESCE(?, sources),
                    scan_frequency = COALESCE(?, scan_frequency),
                    enabled = COALESCE(?, enabled),
                    updated_at = NOW()
                WHERE id = ?
                """,
                (String) body.get("name"),
                (String) body.get("description"),
                (String) body.get("keywords"),
                (String) body.get("negativeKeywords"),
                (String) body.get("sources"),
                (String) body.get("scanFrequency"),
                body.get("enabled") != null ? (Boolean) body.get("enabled") : null,
                id);

        if (updated == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("id", id, "updated", true));
    }

    /**
     * DELETE /api/categories/{id} — delete a category.
     */
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        int deleted = jdbcTemplate.update("DELETE FROM categories WHERE id = ?", id);
        return deleted > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * GET /api/scanners — list available scanner plugins.
     */
    @GetMapping("/scanners")
    public List<Map<String, String>> listScanners() {
        return scannerRegistry.getAllScanners().stream()
                .map(scanner -> Map.of(
                        "type", scanner.getSourceType(),
                        "displayName", scanner.getDisplayName()
                ))
                .toList();
    }
}
