package com.topicscanner.queue;

import com.cncf.scanner.model.PipelineJob;
import com.cncf.scanner.model.PipelineJob.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtractStageHandlerTest {

    @Mock
    private PipelineOrchestrator pipelineOrchestrator;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @InjectMocks
    private ExtractStageHandler handler;

    @Test
    void getStage_returnsExtract() {
        assertEquals(Stage.EXTRACT, handler.getStage());
    }

    @Test
    void handle_updatesTopicAndEnqueuesAnalyze() {
        PipelineJob job = new PipelineJob(Stage.EXTRACT, 42L);
        job.setId(1L);

        when(pipelineOrchestrator.enqueue(any(), anyLong()))
                .thenReturn(new PipelineJob(Stage.ANALYZE, 42L));

        handler.handle(job);

        verify(jdbcTemplate).update(contains("UPDATE topics"), eq(42L));
        verify(pipelineOrchestrator).enqueue(Stage.ANALYZE, 42L);
    }
}
