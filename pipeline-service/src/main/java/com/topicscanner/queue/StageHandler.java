package com.topicscanner.queue;

import com.cncf.scanner.model.PipelineJob;

/**
 * Handles processing for a specific pipeline stage.
 * Implementations are registered by stage type and invoked by the {@link PipelineOrchestrator}.
 */
public interface StageHandler {

    /**
     * The pipeline stage this handler processes.
     */
    PipelineJob.Stage getStage();

    /**
     * Process a single pipeline job.
     * The orchestrator has already marked the job as PROCESSING.
     * This method should do the work and return normally on success.
     * Throwing any exception will cause the orchestrator to mark the job as failed.
     *
     * @param job the job to process (already claimed/PROCESSING)
     */
    void handle(PipelineJob job);
}
