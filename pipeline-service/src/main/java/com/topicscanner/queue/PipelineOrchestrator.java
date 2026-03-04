package com.topicscanner.queue;

import com.cncf.scanner.model.PipelineJob;
import com.cncf.scanner.model.PipelineJob.Stage;
import com.cncf.scanner.repository.PipelineJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Polls the pipeline_jobs table on a schedule and dispatches work to
 * the appropriate {@link StageHandler} for each stage.
 */
@Component
public class PipelineOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(PipelineOrchestrator.class);

    private final PipelineJobRepository jobRepository;
    private final Map<Stage, StageHandler> handlers = new EnumMap<>(Stage.class);

    @Value("${topicscanner.pipeline.stale-job-minutes:10}")
    private int staleJobMinutes;

    public PipelineOrchestrator(PipelineJobRepository jobRepository,
                                 List<StageHandler> stageHandlers) {
        this.jobRepository = jobRepository;
        for (StageHandler handler : stageHandlers) {
            handlers.put(handler.getStage(), handler);
            logger.info("Registered stage handler: {} -> {}",
                    handler.getStage(), handler.getClass().getSimpleName());
        }
    }

    /**
     * Poll all stages for pending jobs. Runs at configured interval.
     */
    @Scheduled(fixedDelayString = "${topicscanner.pipeline.poll-interval-seconds:30}000")
    public void pollAllStages() {
        for (Stage stage : Stage.values()) {
            if (handlers.containsKey(stage)) {
                pollStage(stage);
            }
        }
    }

    /**
     * Claim and process one job for the given stage.
     * Returns true if a job was processed, false if the queue was empty.
     */
    @Transactional
    public boolean pollStage(Stage stage) {
        Optional<PipelineJob> claimed = jobRepository.claimNextJob(stage.name());
        if (claimed.isEmpty()) {
            return false;
        }

        PipelineJob job = claimed.get();
        StageHandler handler = handlers.get(stage);

        try {
            MDC.put("jobId", String.valueOf(job.getId()));
            MDC.put("topicId", String.valueOf(job.getTopicId()));
            MDC.put("stage", stage.name());

            logger.info("Processing job {} for topic {} at stage {}",
                    job.getId(), job.getTopicId(), stage);

            handler.handle(job);

            job.markCompleted();
            jobRepository.save(job);

            logger.info("Completed job {} for topic {} at stage {}",
                    job.getId(), job.getTopicId(), stage);
            return true;
        } catch (Exception e) {
            logger.error("Failed job {} for topic {} at stage {}: {}",
                    job.getId(), job.getTopicId(), stage, e.getMessage(), e);

            job.markFailed(e.getMessage());
            jobRepository.save(job);
            return true; // a job was processed (just failed)
        } finally {
            MDC.clear();
        }
    }

    /**
     * Reset jobs that have been stuck in PROCESSING state for too long.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void resetStaleJobs() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(staleJobMinutes);
        int reset = jobRepository.resetStaleJobs(threshold);
        if (reset > 0) {
            logger.warn("Reset {} stale jobs (processing for >{}min)", reset, staleJobMinutes);
        }
    }

    /**
     * Enqueue a new job for the given stage and topic.
     */
    @Transactional
    public PipelineJob enqueue(Stage stage, Long topicId) {
        PipelineJob job = new PipelineJob(stage, topicId);
        PipelineJob saved = jobRepository.save(job);
        logger.debug("Enqueued {} job for topic {}", stage, topicId);
        return saved;
    }

    /**
     * Check if a handler is registered for the given stage.
     */
    public boolean hasHandler(Stage stage) {
        return handlers.containsKey(stage);
    }
}
