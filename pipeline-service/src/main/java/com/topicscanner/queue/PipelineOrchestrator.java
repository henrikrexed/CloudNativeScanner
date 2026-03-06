package com.topicscanner.queue;

import com.cncf.scanner.model.PipelineJob;
import com.cncf.scanner.model.PipelineJob.Stage;
import com.cncf.scanner.repository.PipelineJobRepository;
import com.topicscanner.telemetry.TelemetryService;
import com.topicscanner.telemetry.TracePropagationHelper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final TelemetryService telemetryService;

    public PipelineOrchestrator(PipelineJobRepository jobRepository,
                                 List<StageHandler> stageHandlers,
                                 @Value("${topicscanner.pipeline.stale-job-minutes:10}") int staleJobMinutes,
                                 @Autowired(required = false) TelemetryService telemetryService) {
        this.jobRepository = jobRepository;
        this.staleJobMinutes = staleJobMinutes;
        this.telemetryService = telemetryService;
        for (StageHandler handler : stageHandlers) {
            handlers.put(handler.getStage(), handler);
            logger.info("Registered stage handler: {} -> {}",
                    handler.getStage(), handler.getClass().getSimpleName());
        }
    }

    /**
     * Poll all stages for pending jobs.
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
     */
    public boolean pollStage(Stage stage) {
        Optional<PipelineJob> claimed = claimJob(stage);
        if (claimed.isEmpty()) {
            return false;
        }

        PipelineJob job = claimed.get();
        StageHandler handler = handlers.get(stage);

        // Extract trace context from job metadata for distributed tracing
        Context parentContext = TracePropagationHelper.extractTraceContext(job.getMetadata());

        Span stageSpan = telemetryService != null
                ? telemetryService.getTracer()
                    .spanBuilder("topicscanner.pipeline.stage." + stage.name().toLowerCase())
                    .setParent(parentContext)
                    .startSpan()
                : null;

        try (Scope ignored = stageSpan != null ? stageSpan.makeCurrent() : null) {
            MDC.put("jobId", String.valueOf(job.getId()));
            MDC.put("topicId", String.valueOf(job.getTopicId()));
            MDC.put("stage", stage.name());

            if (stageSpan != null) {
                stageSpan.setAttribute("topicId", job.getTopicId());
                stageSpan.setAttribute("stage", stage.name());
            }

            logger.info("Processing job {} for topic {} at stage {}",
                    job.getId(), job.getTopicId(), stage);

            handler.handle(job);

            completeJob(job);

            if (stageSpan != null) {
                stageSpan.setAttribute("result", "success");
                stageSpan.setStatus(StatusCode.OK);
            }

            logger.info("Completed job {} for topic {} at stage {}",
                    job.getId(), job.getTopicId(), stage);
            return true;
        } catch (Exception e) {
            logger.error("Failed job {} for topic {} at stage {}: {}",
                    job.getId(), job.getTopicId(), stage, e.getMessage(), e);

            failJob(job, e.getMessage());

            if (stageSpan != null) {
                stageSpan.setAttribute("result", "failure");
                stageSpan.setStatus(StatusCode.ERROR, e.getMessage());
                stageSpan.recordException(e);
            }
            return true;
        } finally {
            if (stageSpan != null) stageSpan.end();
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

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void resetStaleJobs() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(staleJobMinutes);
        int reset = jobRepository.resetStaleJobs(threshold);
        if (reset > 0) {
            logger.warn("Reset {} stale jobs (processing for >{}min)", reset, staleJobMinutes);
        }
    }

    @Transactional
    public PipelineJob enqueue(Stage stage, Long topicId) {
        PipelineJob job = new PipelineJob(stage, topicId);
        PipelineJob saved = jobRepository.save(job);
        logger.debug("Enqueued {} job for topic {}", stage, topicId);
        return saved;
    }

    public boolean hasHandler(Stage stage) {
        return handlers.containsKey(stage);
    }
}
