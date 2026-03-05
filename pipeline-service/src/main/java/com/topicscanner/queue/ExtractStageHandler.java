package com.topicscanner.queue;

import com.cncf.scanner.model.PipelineJob;
import com.cncf.scanner.model.PipelineJob.Stage;
import com.topicscanner.extraction.ContentExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Handles the EXTRACT stage: fetches full content from discovered URLs.
 * Uses ContentExtractionService to extract content based on source type.
 * After extraction, enqueues an ANALYZE job for the topic.
 */
@Component
public class ExtractStageHandler implements StageHandler {

    private static final Logger logger = LoggerFactory.getLogger(ExtractStageHandler.class);

    private final PipelineOrchestrator pipelineOrchestrator;
    private final ContentExtractionService contentExtractionService;
    private final JdbcTemplate jdbcTemplate;

    public ExtractStageHandler(@Lazy PipelineOrchestrator pipelineOrchestrator,
                                ContentExtractionService contentExtractionService,
                                JdbcTemplate jdbcTemplate) {
        this.pipelineOrchestrator = pipelineOrchestrator;
        this.contentExtractionService = contentExtractionService;
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

        // Fetch URL and source type for this topic
        var topicInfo = jdbcTemplate.queryForMap(
                "SELECT url, source_type FROM topics t JOIN sources s ON t.source_id = s.id WHERE t.id = ?",
                topicId);

        String url = (String) topicInfo.get("url");
        String sourceType = (String) topicInfo.get("source_type");

        // Extract content
        Optional<String> content = contentExtractionService.extract(url, sourceType);

        if (content.isPresent()) {
            jdbcTemplate.update(
                    """
                    UPDATE topics
                    SET pipeline_stage = 'extracted',
                        extraction_status = 'COMPLETED',
                        extracted_content = ?,
                        updated_at = NOW()
                    WHERE id = ?
                    """,
                    content.get(), topicId);

            logger.info("Extraction complete for topic {} ({} chars)", topicId, content.get().length());
        } else {
            jdbcTemplate.update(
                    """
                    UPDATE topics
                    SET pipeline_stage = 'extracted',
                        extraction_status = 'FAILED',
                        updated_at = NOW()
                    WHERE id = ?
                    """,
                    topicId);

            logger.warn("Extraction failed for topic {} ({})", topicId, url);
        }

        // Chain to next stage regardless — ANALYZE will check extraction_status
        pipelineOrchestrator.enqueue(Stage.ANALYZE, topicId);
    }
}
