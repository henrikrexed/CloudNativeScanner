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
class AnalyzeStageHandlerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @InjectMocks
    private AnalyzeStageHandler handler;

    @Test
    void getStage_returnsAnalyze() {
        assertEquals(Stage.ANALYZE, handler.getStage());
    }

    @Test
    void handle_updatesTopicToAnalyzed() {
        PipelineJob job = new PipelineJob(Stage.ANALYZE, 42L);
        job.setId(1L);

        handler.handle(job);

        verify(jdbcTemplate).update(contains("analyzed"), eq(42L));
    }
}
