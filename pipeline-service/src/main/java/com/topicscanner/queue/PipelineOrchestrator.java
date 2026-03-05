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
    private final int staleJobMinutes;

    public PipelineOrchestrator(PipelineJobRepository jobRepository,
                                 List<StageHandler> stageHandlers,
                                 @Value("${topicscanner.pipeline.stale-job-minutes:10}") int staleJobMinutes) {
        this.jobRepository = jobRepository;
        this.staleJobMinutes = staleJobMinutes;
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
     * Uses separate transactions for claim, completion, and failure
     * to prevent failure state loss on DB errors.
     */
    public boolean pollStage(Stage stage) {
        Optional<PipelineJob> claimed = claimJob(stage);
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

            completeJob(job);

            logger.info("Completed job {} for topic {} at stage {}",
                    job.getId(), job.getTopicId(), stage);
            return true;
        } catch (Exception e) {
            logger.error("Failed job {} for topic {} at stage {}: {}",
                    job.getId(), job.getTopicId(), stage, e.getMessage(), e);

            failJob(job, e.getMessage());
            return true;
        } finally {
            MDC.remove("jobId");
            MDC.remove("topicId");
            MDC.remove("stage");
        }
    }

    @Transactional
    protected Optional<PipelineJob> claimJob(Stage stage) {
        return jobRepository.claimNextJob(stage.name());
    }

    @Transactional
    protected void completeJob(PipelineJob job) {
        job.markCompleted();
        jobRepository.save(job);
    }

    @Transactional
    protected void failJob(PipelineJob job, String errorMessage) {
        job.markFailed(errorMessage);
        jobRepository.save(job);
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
