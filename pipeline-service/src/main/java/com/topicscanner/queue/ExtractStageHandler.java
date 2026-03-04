package com.topicscanner.queue;

import com.cncf.scanner.model.PipelineJob;
import com.cncf.scanner.model.PipelineJob.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Handles the EXTRACT stage: fetches full content from discovered URLs.
 * After extraction, enqueues an ANALYZE job for the topic.
 * <p>
 * This is currently a stub that marks topics as extracted and chains to ANALYZE.
 * Full extraction logic (JSoup, YouTube transcripts, etc.) will be added in E6.
 */
@Component
public class ExtractStageHandler implements StageHandler {

    private static final Logger logger = LoggerFactory.getLogger(ExtractStageHandler.class);

    private final PipelineOrchestrator pipelineOrchestrator;
    private final JdbcTemplate jdbcTemplate;

    public ExtractStageHandler(@Lazy PipelineOrchestrator pipelineOrchestrator,
                                JdbcTemplate jdbcTemplate) {
        this.pipelineOrchestrator = pipelineOrchestrator;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Stage getStage() {
        return Stage.EXTRACT;
    }

    @Override
    public void handle(PipelineJob job) {
        Long topicId = job.getTopicId();
        logger.info("Extracting content for topic {}", topicId);

        // Update topic pipeline stage and extraction status
        jdbcTemplate.update(
                """
                UPDATE topics
                SET pipeline_stage = 'extracted',
                    extraction_status = 'COMPLETED',
                    updated_at = NOW()
                WHERE id = ?
                """,
                topicId);

        // Chain to next stage
        pipelineOrchestrator.enqueue(Stage.ANALYZE, topicId);

        logger.info("Extraction complete for topic {}, ANALYZE job enqueued", topicId);
    }
}
