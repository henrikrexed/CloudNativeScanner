package com.topicscanner.queue;

import com.cncf.scanner.model.PipelineJob;
import com.cncf.scanner.model.PipelineJob.Stage;
import com.topicscanner.filter.FilterPipeline;
import com.topicscanner.filter.FilterResult;
import com.topicscanner.filter.TopicContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Handles the ANALYZE stage: runs the 7-stage filter pipeline on extracted content.
 * Topics that pass all filters are marked as 'analyzed'.
 * Topics that fail are marked as 'rejected' with the rejection reason.
 */
@Component
public class AnalyzeStageHandler implements StageHandler {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzeStageHandler.class);

    private final FilterPipeline filterPipeline;
    private final JdbcTemplate jdbcTemplate;

    public AnalyzeStageHandler(FilterPipeline filterPipeline, JdbcTemplate jdbcTemplate) {
        this.filterPipeline = filterPipeline;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Stage getStage() {
        return Stage.ANALYZE;
    }

    @Override
    public void handle(PipelineJob job) {
        Long topicId = job.getTopicId();
        logger.info("Analyzing topic {}", topicId);

        // Load topic data for the filter pipeline
        TopicContext context = loadTopicContext(topicId);
        if (context == null) {
            logger.warn("Topic {} not found, skipping analysis", topicId);
            return;
        }

        // Run filter pipeline
        FilterPipeline.PipelineResult result = filterPipeline.execute(context);

        if (result.passed()) {
            // Topic passed all filters — persist scores and mark analyzed
            Double relevanceScore = context.getAttribute("relevanceScore");
            Double qualityScore = context.getAttribute("qualityScore");
            String embeddingVector = context.getAttribute("embeddingVector");

            if (embeddingVector != null) {
                jdbcTemplate.update(
                        """
                        UPDATE topics
                        SET pipeline_stage = 'analyzed',
                            relevance_score = ?,
                            quality_score = ?,
                            embedding = ?::vector,
                            updated_at = NOW()
                        WHERE id = ?
                        """,
                        relevanceScore, qualityScore, embeddingVector, topicId);
            } else {
                jdbcTemplate.update(
                        """
                        UPDATE topics
                        SET pipeline_stage = 'analyzed',
                            relevance_score = ?,
                            quality_score = ?,
                            updated_at = NOW()
                        WHERE id = ?
                        """,
                        relevanceScore, qualityScore, topicId);
            }

            logger.info("Topic {} passed analysis (relevance={}, quality={})",
                    topicId, relevanceScore, qualityScore);
        } else {
            // Topic rejected — store reason
            String rejectionReason = result.filterResults().stream()
                    .filter(r -> !r.passed())
                    .findFirst()
                    .map(r -> r.filterName() + ": " + r.reason())
                    .orElse("unknown");

            jdbcTemplate.update(
                    """
                    UPDATE topics
                    SET pipeline_stage = 'rejected',
                        rejection_reason = ?,
                        updated_at = NOW()
                    WHERE id = ?
                    """,
                    rejectionReason, topicId);

            logger.info("Topic {} rejected: {}", topicId, rejectionReason);
        }
    }

    private TopicContext loadTopicContext(Long topicId) {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    """
                    SELECT t.id, t.title, t.url, t.extracted_content,
                           s.source_type,
                           c.name AS category_name,
                           c.keywords AS category_keywords,
                           c.negative_keywords
                    FROM topics t
                    JOIN sources s ON t.source_id = s.id
                    LEFT JOIN categories c ON t.category_id = c.id
                    WHERE t.id = ?
                    """,
                    topicId);

            String categoryKeywordsJson = (String) row.get("category_keywords");
            String negativeKeywordsJson = (String) row.get("negative_keywords");

            return new TopicContext(
                    topicId,
                    (String) row.get("title"),
                    (String) row.get("url"),
                    (String) row.get("source_type"),
                    (String) row.get("extracted_content"),
                    (String) row.get("category_name"),
                    parseJsonList(categoryKeywordsJson),
                    parseJsonList(negativeKeywordsJson)
            );
        } catch (Exception e) {
            logger.error("Failed to load topic context for {}: {}", topicId, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}
