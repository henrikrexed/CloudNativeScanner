package com.topicscanner.queue;

import com.cncf.scanner.model.Category;
import com.cncf.scanner.model.PipelineJob;
import com.cncf.scanner.model.PipelineJob.Stage;
import com.cncf.scanner.service.CategoryService;
import com.topicscanner.scanner.ScanRequest;
import com.topicscanner.scanner.ScanResult;
import com.topicscanner.scanner.ScannerRegistry;
import com.topicscanner.scanner.SourceScanner;
import com.topicscanner.telemetry.TelemetryService;
import com.topicscanner.telemetry.TracePropagationHelper;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final TelemetryService telemetryService;

    public ScanOrchestrator(CategoryService categoryService,
                             ScannerRegistry scannerRegistry,
                             PipelineOrchestrator pipelineOrchestrator,
                             JdbcTemplate jdbcTemplate,
                             @Value("${topicscanner.pipeline.max-results-per-scan:25}") int maxResultsPerScan,
                             @Autowired(required = false) TelemetryService telemetryService) {
        this.categoryService = categoryService;
        this.scannerRegistry = scannerRegistry;
        this.pipelineOrchestrator = pipelineOrchestrator;
        this.jdbcTemplate = jdbcTemplate;
        this.maxResultsPerScan = maxResultsPerScan;
        this.telemetryService = telemetryService;
    }

    /**
     * Scheduled scan of all enabled categories.
     */
    @Scheduled(cron = "${topicscanner.pipeline.scan-cron:0 0 6 * * *}")
    public void scheduledScan() {
        Span rootSpan = telemetryService != null
                ? telemetryService.getTracer().spanBuilder("topicscanner.scan").startSpan()
                : null;
        try (Scope ignored = rootSpan != null ? rootSpan.makeCurrent() : null) {
            doScheduledScan();
            if (rootSpan != null) rootSpan.setStatus(StatusCode.OK);
        } catch (Exception e) {
            if (rootSpan != null) {
                rootSpan.setStatus(StatusCode.ERROR, e.getMessage());
                rootSpan.recordException(e);
            }
            throw e;
        } finally {
            if (rootSpan != null) rootSpan.end();
        }
    }

    private void doScheduledScan() {
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
        String sourceType = scanner.getSourceType();
        Span sourceSpan = telemetryService != null
                ? telemetryService.getTracer().spanBuilder("topicscanner.scan." + sourceType).startSpan()
                : null;

        try (Scope ignored = sourceSpan != null ? sourceSpan.makeCurrent() : null) {
            if (sourceSpan != null) {
                sourceSpan.setAttribute("source", sourceType);
                sourceSpan.setAttribute("category", category.getName());
            }

            ScanRequest request = new ScanRequest(keywords, negativeKeywords, null, maxResultsPerScan);
            List<ScanResult> results = scanner.scan(request);

            logger.info("Scanner '{}' returned {} results for category '{}'",
                    sourceType, results.size(), category.getName());

            int newCount = 0;
            for (ScanResult result : results) {
                Long topicId = persistTopicIfNew(result, category);
                if (topicId != null) {
                    PipelineJob job = pipelineOrchestrator.enqueue(Stage.EXTRACT, topicId);
                    TracePropagationHelper.injectTraceContext(job.getMetadata());
                    newCount++;
                }
            }

            if (sourceSpan != null) {
                sourceSpan.setAttribute("topics_found", newCount);
                sourceSpan.setStatus(StatusCode.OK);
            }
            return newCount;
        } catch (Exception e) {
            if (sourceSpan != null) {
                sourceSpan.setStatus(StatusCode.ERROR, e.getMessage());
                sourceSpan.recordException(e);
            }
            throw e;
        } finally {
            if (sourceSpan != null) sourceSpan.end();
        }
    }

    /**
     * Atomically insert a topic if it doesn't already exist (by source + external_id).
     */
    private Long persistTopicIfNew(ScanResult result, Category category) {
        String externalId = deriveExternalId(result);

        List<Long> sourceIds = jdbcTemplate.queryForList(
                "SELECT id FROM sources WHERE LOWER(name) = LOWER(?)",
                Long.class, result.sourceType());

        if (sourceIds.isEmpty()) {
            logger.warn("Source '{}' not found in DB, skipping topic '{}'",
                    result.sourceType(), result.title());
            return null;
        }

        Long sourceId = sourceIds.get(0);

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
