package com.cncf.scanner.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PipelineJobTest {

    @Test
    void constructor_setsStageAndTopicId() {
        var job = new PipelineJob(PipelineJob.Stage.EXTRACT, 42L);

        assertEquals(PipelineJob.Stage.EXTRACT, job.getStage());
        assertEquals(42L, job.getTopicId());
        assertEquals(PipelineJob.Status.PENDING, job.getStatus());
        assertEquals(0, job.getRetryCount());
        assertEquals(3, job.getMaxRetries());
    }

    @Test
    void markProcessing_setsStatusAndClaimedAt() {
        var job = new PipelineJob(PipelineJob.Stage.ANALYZE, 1L);

        job.markProcessing();

        assertEquals(PipelineJob.Status.PROCESSING, job.getStatus());
        assertNotNull(job.getClaimedAt());
    }

    @Test
    void markCompleted_setsStatusAndCompletedAt() {
        var job = new PipelineJob(PipelineJob.Stage.GENERATE, 1L);
        job.markProcessing();

        job.markCompleted();

        assertEquals(PipelineJob.Status.COMPLETED, job.getStatus());
        assertNotNull(job.getCompletedAt());
    }

    @Test
    void markFailed_firstFailure_resetsToRetry() {
        var job = new PipelineJob(PipelineJob.Stage.EXTRACT, 1L);
        job.markProcessing();

        job.markFailed("Connection timeout");

        assertEquals(PipelineJob.Status.PENDING, job.getStatus());
        assertEquals(1, job.getRetryCount());
        assertNull(job.getClaimedAt());
        assertEquals("Connection timeout", job.getErrorMessage());
    }

    @Test
    void markFailed_exhaustsRetries_setsFailed() {
        var job = new PipelineJob(PipelineJob.Stage.EXTRACT, 1L);
        job.setMaxRetries(2);

        job.markFailed("Error 1");
        assertEquals(PipelineJob.Status.PENDING, job.getStatus());
        assertEquals(1, job.getRetryCount());

        job.markFailed("Error 2");
        assertEquals(PipelineJob.Status.FAILED, job.getStatus());
        assertEquals(2, job.getRetryCount());
        assertEquals("Error 2", job.getErrorMessage());
    }

    @Test
    void markFailed_maxRetriesThree_failsOnThird() {
        var job = new PipelineJob(PipelineJob.Stage.ANALYZE, 1L);
        // default maxRetries = 3

        job.markFailed("Attempt 1");
        assertTrue(job.canRetry());

        job.markFailed("Attempt 2");
        assertTrue(job.canRetry());

        job.markFailed("Attempt 3");
        assertFalse(job.canRetry());
        assertEquals(PipelineJob.Status.FAILED, job.getStatus());
    }

    @Test
    void canRetry_newJob_returnsTrue() {
        var job = new PipelineJob(PipelineJob.Stage.EXTRACT, 1L);
        assertTrue(job.canRetry());
    }

    @Test
    void canRetry_afterMaxRetries_returnsFalse() {
        var job = new PipelineJob(PipelineJob.Stage.EXTRACT, 1L);
        job.setMaxRetries(1);

        job.markFailed("Final error");

        assertFalse(job.canRetry());
    }

    @Test
    void defaultConstructor_forJPA() {
        var job = new PipelineJob();
        assertNull(job.getId());
        assertNull(job.getStage());
        assertEquals(PipelineJob.Status.PENDING, job.getStatus());
    }
}
