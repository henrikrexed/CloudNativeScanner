package com.topicscanner.queue;

import com.cncf.scanner.model.PipelineJob;
import com.cncf.scanner.model.PipelineJob.Stage;
import com.topicscanner.extraction.ContentExtractionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtractStageHandlerTest {

    @Mock
    private PipelineOrchestrator pipelineOrchestrator;
    @Mock
    private ContentExtractionService contentExtractionService;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @InjectMocks
    private ExtractStageHandler handler;

    @Test
    void getStage_returnsExtract() {
        assertEquals(Stage.EXTRACT, handler.getStage());
    }

    @Test
    void handle_extractsContentAndUpdatesTopicOnSuccess() {
        PipelineJob job = new PipelineJob(Stage.EXTRACT, 42L);
        job.setId(1L);

        when(jdbcTemplate.queryForList(contains("SELECT url"), eq(42L)))
                .thenReturn(List.of(Map.of("url", "https://example.com/post", "source_type", "medium")));
        when(contentExtractionService.extract("https://example.com/post", "medium"))
                .thenReturn(Optional.of("Extracted content here"));
        when(pipelineOrchestrator.enqueue(any(), anyLong()))
                .thenReturn(new PipelineJob(Stage.ANALYZE, 42L));

        handler.handle(job);

        verify(jdbcTemplate).update(contains("COMPLETED"), eq("Extracted content here"), eq(42L));
        verify(pipelineOrchestrator).enqueue(Stage.ANALYZE, 42L);
    }

    @Test
    void handle_marksExtractionFailedWhenContentEmpty() {
        PipelineJob job = new PipelineJob(Stage.EXTRACT, 42L);
        job.setId(1L);

        when(jdbcTemplate.queryForList(contains("SELECT url"), eq(42L)))
                .thenReturn(List.of(Map.of("url", "https://example.com/paywalled", "source_type", "medium")));
        when(contentExtractionService.extract("https://example.com/paywalled", "medium"))
                .thenReturn(Optional.empty());
        when(pipelineOrchestrator.enqueue(any(), anyLong()))
                .thenReturn(new PipelineJob(Stage.ANALYZE, 42L));

        handler.handle(job);

        verify(jdbcTemplate).update(contains("FAILED"), eq(42L));
        verify(pipelineOrchestrator).enqueue(Stage.ANALYZE, 42L);
    }
}
