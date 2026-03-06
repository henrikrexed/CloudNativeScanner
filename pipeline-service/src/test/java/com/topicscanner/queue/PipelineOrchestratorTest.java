package com.topicscanner.queue;

import com.cncf.scanner.model.PipelineJob;
import com.cncf.scanner.model.PipelineJob.Stage;
import com.cncf.scanner.model.PipelineJob.Status;
import com.cncf.scanner.repository.PipelineJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineOrchestratorTest {

    @Mock
    private PipelineJobRepository jobRepository;

    private PipelineOrchestrator orchestrator;
    private TestStageHandler extractHandler;

    static class TestStageHandler implements StageHandler {
        private final Stage stage;
        private boolean shouldFail;
        private int handleCount;

        TestStageHandler(Stage stage) {
            this.stage = stage;
        }

        @Override
        public Stage getStage() {
            return stage;
        }

        @Override
        public void handle(PipelineJob job) {
            handleCount++;
            if (shouldFail) {
                throw new RuntimeException("Simulated failure");
            }
        }

        void setShouldFail(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }

        int getHandleCount() {
            return handleCount;
        }
    }

    @BeforeEach
    void setUp() {
        extractHandler = new TestStageHandler(Stage.EXTRACT);
        orchestrator = new PipelineOrchestrator(jobRepository, List.of(extractHandler), 10);
    }

    @Test
    void pollStage_noJobs_returnsFalse() {
        when(jobRepository.claimNextJob("EXTRACT")).thenReturn(Optional.empty());

        boolean result = orchestrator.pollStage(Stage.EXTRACT);

        assertFalse(result);
        assertEquals(0, extractHandler.getHandleCount());
    }

    @Test
    void pollStage_jobAvailable_processesAndCompletes() {
        PipelineJob job = new PipelineJob(Stage.EXTRACT, 42L);
        job.setId(1L);
        when(jobRepository.claimNextJob("EXTRACT")).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean result = orchestrator.pollStage(Stage.EXTRACT);

        assertTrue(result);
        assertEquals(1, extractHandler.getHandleCount());

        ArgumentCaptor<PipelineJob> captor = ArgumentCaptor.forClass(PipelineJob.class);
        verify(jobRepository).save(captor.capture());
        assertEquals(Status.COMPLETED, captor.getValue().getStatus());
    }

    @Test
    void pollStage_handlerFails_marksJobFailed() {
        extractHandler.setShouldFail(true);

        PipelineJob job = new PipelineJob(Stage.EXTRACT, 42L);
        job.setId(1L);
        when(jobRepository.claimNextJob("EXTRACT")).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean result = orchestrator.pollStage(Stage.EXTRACT);

        assertTrue(result);

        ArgumentCaptor<PipelineJob> captor = ArgumentCaptor.forClass(PipelineJob.class);
        verify(jobRepository).save(captor.capture());
        PipelineJob saved = captor.getValue();
        // First failure with default maxRetries=3 should reset to PENDING for retry
        assertEquals(Status.PENDING, saved.getStatus());
        assertEquals(1, saved.getRetryCount());
        assertEquals("Simulated failure", saved.getErrorMessage());
    }

    @Test
    void pollStage_handlerFails_exhaustsRetries_setsFailed() {
        extractHandler.setShouldFail(true);

        PipelineJob job = new PipelineJob(Stage.EXTRACT, 42L);
        job.setId(1L);
        job.setRetryCount(2); // already retried twice
        job.setMaxRetries(3);
        when(jobRepository.claimNextJob("EXTRACT")).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orchestrator.pollStage(Stage.EXTRACT);

        ArgumentCaptor<PipelineJob> captor = ArgumentCaptor.forClass(PipelineJob.class);
        verify(jobRepository).save(captor.capture());
        assertEquals(Status.FAILED, captor.getValue().getStatus());
    }

    @Test
    void enqueue_createsNewJob() {
        when(jobRepository.save(any())).thenAnswer(inv -> {
            PipelineJob j = inv.getArgument(0);
            j.setId(99L);
            return j;
        });

        PipelineJob result = orchestrator.enqueue(Stage.ANALYZE, 7L);

        assertEquals(Stage.ANALYZE, result.getStage());
        assertEquals(7L, result.getTopicId());
        assertEquals(Status.PENDING, result.getStatus());
        verify(jobRepository).save(any(PipelineJob.class));
    }

    @Test
    void hasHandler_registeredStage_returnsTrue() {
        assertTrue(orchestrator.hasHandler(Stage.EXTRACT));
    }

    @Test
    void hasHandler_unregisteredStage_returnsFalse() {
        assertFalse(orchestrator.hasHandler(Stage.GENERATE));
    }

    @Test
    void resetStaleJobs_callsRepository() {
        when(jobRepository.resetStaleJobs(any())).thenReturn(2);

        orchestrator.resetStaleJobs();

        verify(jobRepository).resetStaleJobs(any());
    }

    @Test
    void pollAllStages_onlyPollsRegisteredHandlers() {
        when(jobRepository.claimNextJob("EXTRACT")).thenReturn(Optional.empty());

        orchestrator.pollAllStages();

        // Only EXTRACT handler is registered, so only EXTRACT is polled
        verify(jobRepository).claimNextJob("EXTRACT");
        verify(jobRepository, never()).claimNextJob("ANALYZE");
        verify(jobRepository, never()).claimNextJob("GENERATE");
    }
}
