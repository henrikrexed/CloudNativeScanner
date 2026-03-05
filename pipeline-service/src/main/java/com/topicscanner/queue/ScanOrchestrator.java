package com.topicscanner.queue;

import com.cncf.scanner.model.Category;
import com.cncf.scanner.model.PipelineJob.Stage;
import com.cncf.scanner.service.CategoryService;
import com.topicscanner.scanner.ScanRequest;
import com.topicscanner.scanner.ScanResult;
import com.topicscanner.scanner.ScannerRegistry;
import com.topicscanner.scanner.SourceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;

/**
 * Runs on a cron schedule. For each enabled category, finds matching scanners,
 * executes scans, persists new topics, and enqueues EXTRACT jobs.
 */
@Component
public class ScanOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(ScanOrchestrator.class);

    private final CategoryService categoryService;
    private final ScannerRegistry scannerRegistry;
    private final PipelineOrchestrator pipelineOrchestrator;
    private final JdbcTemplate jdbcTemplate;
    private final int maxResultsPerScan;

    public ScanOrchestrator(CategoryService categoryService,
                             ScannerRegistry scannerRegistry,
                             PipelineOrchestrator pipelineOrchestrator,
                             JdbcTemplate jdbcTemplate,
                             @Value("${topicscanner.pipeline.max-results-per-scan:25}") int maxResultsPerScan) {
        this.categoryService = categoryService;
        this.scannerRegistry = scannerRegistry;
        this.pipelineOrchestrator = pipelineOrchestrator;
        this.jdbcTemplate = jdbcTemplate;
        this.maxResultsPerScan = maxResultsPerScan;
    }

    /**
     * Scheduled scan of all enabled categories.
     */
    @Scheduled(cron = "${topicscanner.pipeline.scan-cron:0 0 6 * * *}")
    public void scheduledScan() {
        logger.info("Starting scheduled scan");
        List<Category> categories = categoryService.findEnabled();
        int totalNew = 0;

        for (Category category : categories) {
            try {
                MDC.put("categoryId", String.valueOf(category.getId()));
                MDC.put("category", category.getName());
                int found = scanCategory(category);
                totalNew += found;
            } catch (Exception e) {
                logger.error("Failed to scan category '{}': {}", category.getName(), e.getMessage(), e);
            } finally {
                MDC.remove("categoryId");
                MDC.remove("category");
            }
        }

        logger.info("Scheduled scan complete: {} new topics from {} categories",
                totalNew, categories.size());
    }

    /**
     * Scan a single category across all its configured sources.
     * No @Transactional here — avoids holding a DB connection during external HTTP calls.
     * Each topic insert is atomic via upsert.
     */
    public int scanCategory(Category category) {
        List<String> sources = category.getSourcesList();
        List<String> keywords = category.getKeywordsList();
        List<String> negativeKeywords = category.getNegativeKeywordsList();

        if (keywords.isEmpty()) {
            logger.warn("Category '{}' has no keywords, skipping", category.getName());
            return 0;
        }

        int newTopics = 0;

        for (String sourceType : sources) {
            var scanner = scannerRegistry.getScanner(sourceType.toLowerCase());
            if (scanner.isEmpty()) {
                logger.debug("No scanner registered for source '{}', skipping", sourceType);
                continue;
            }

            try {
                newTopics += scanSource(scanner.get(), category, keywords, negativeKeywords);
            } catch (Exception e) {
                logger.error("Scanner '{}' failed for category '{}': {}",
                        sourceType, category.getName(), e.getMessage(), e);
            }
        }

        return newTopics;
    }

    private int scanSource(SourceScanner scanner, Category category,
                            List<String> keywords, List<String> negativeKeywords) {
        ScanRequest request = new ScanRequest(keywords, negativeKeywords, null, maxResultsPerScan);
        List<ScanResult> results = scanner.scan(request);

        logger.info("Scanner '{}' returned {} results for category '{}'",
                scanner.getSourceType(), results.size(), category.getName());

        int newCount = 0;
        for (ScanResult result : results) {
            Long topicId = persistTopicIfNew(result, category);
            if (topicId != null) {
                pipelineOrchestrator.enqueue(Stage.EXTRACT, topicId);
                newCount++;
            }
        }

        return newCount;
    }

    /**
     * Atomically insert a topic if it doesn't already exist (by source + external_id).
     * Uses INSERT ... ON CONFLICT DO NOTHING to avoid TOCTOU race conditions.
     * Returns the topic ID if newly inserted, null if it already existed.
     */
    private Long persistTopicIfNew(ScanResult result, Category category) {
        String externalId = deriveExternalId(result);

        // Look up source ID by scanner type name
        List<Long> sourceIds = jdbcTemplate.queryForList(
                "SELECT id FROM sources WHERE LOWER(name) = LOWER(?)",
                Long.class, result.sourceType());

        if (sourceIds.isEmpty()) {
            logger.warn("Source '{}' not found in DB, skipping topic '{}'",
                    result.sourceType(), result.title());
            return null;
        }

        Long sourceId = sourceIds.get(0);

        // Atomic upsert — avoids TOCTOU race between concurrent scanners
        List<Long> inserted = jdbcTemplate.queryForList(
                """
                INSERT INTO topics (source_id, category_id, external_id, title, url,
                                    pipeline_stage, source_date, collected_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'scanned', ?, NOW(), NOW(), NOW())
                ON CONFLICT (source_id, external_id) DO NOTHING
                RETURNING id
                """,
                Long.class,
                sourceId,
                category.getId(),
                externalId,
                result.title(),
                result.url(),
                result.sourceDate() != null ? Timestamp.valueOf(result.sourceDate()) : null
        );

        return inserted.isEmpty() ? null : inserted.get(0);
    }

    /**
     * Derive a stable external ID from the scan result.
     */
    private String deriveExternalId(ScanResult result) {
        if (result.metadata() != null) {
            Object id = result.metadata().get("externalId");
            if (id != null && !id.toString().isBlank()) {
                return id.toString();
            }
        }
        return result.url();
    }
}
