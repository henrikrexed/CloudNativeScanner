package com.topicscanner.queue;

import com.cncf.scanner.model.PipelineJob;
import com.cncf.scanner.model.PipelineJob.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Handles the ANALYZE stage: runs the filter + enrich pipeline on extracted content.
 * <p>
 * This is currently a stub that marks topics as analyzed.
 * Full filter pipeline (language, relevance, quality, classification, summarization,
 * embedding) will be added in E5.
 */
@Component
public class AnalyzeStageHandler implements StageHandler {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzeStageHandler.class);

    private final JdbcTemplate jdbcTemplate;

    public AnalyzeStageHandler(JdbcTemplate jdbcTemplate) {
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

        // Update topic pipeline stage to final state
        jdbcTemplate.update(
                """
                UPDATE topics
                SET pipeline_stage = 'analyzed',
                    updated_at = NOW()
                WHERE id = ?
                """,
                topicId);

        logger.info("Analysis complete for topic {}", topicId);
    }
}
