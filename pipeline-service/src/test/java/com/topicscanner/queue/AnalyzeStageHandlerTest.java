package com.topicscanner.queue;

import com.cncf.scanner.model.PipelineJob;
import com.cncf.scanner.model.PipelineJob.Stage;
import com.topicscanner.filter.FilterPipeline;
import com.topicscanner.filter.FilterResult;
import com.topicscanner.filter.TopicContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyzeStageHandlerTest {

    @Mock
    private FilterPipeline filterPipeline;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @InjectMocks
    private AnalyzeStageHandler handler;

    @Test
    void getStage_returnsAnalyze() {
        assertEquals(Stage.ANALYZE, handler.getStage());
    }

    @Test
    void handle_passedTopicMarkedAnalyzed() {
        PipelineJob job = new PipelineJob(Stage.ANALYZE, 42L);
        job.setId(1L);

        when(jdbcTemplate.queryForMap(contains("SELECT"), eq(42L)))
                .thenReturn(Map.of(
                        "title", "Test Topic",
                        "url", "https://example.com",
                        "source_type", "medium",
                        "category_name", "Tech"
                ));

        when(filterPipeline.execute(any(TopicContext.class)))
                .thenReturn(new FilterPipeline.PipelineResult(true, List.of(
                        FilterResult.pass("test", 0.9)
                )));

        handler.handle(job);

        verify(jdbcTemplate).update(contains("analyzed"), any(), any(), eq(42L));
    }

    @Test
    void handle_rejectedTopicMarkedRejected() {
        PipelineJob job = new PipelineJob(Stage.ANALYZE, 42L);
        job.setId(1L);

        when(jdbcTemplate.queryForMap(contains("SELECT"), eq(42L)))
                .thenReturn(Map.of(
                        "title", "Bad Topic",
                        "url", "https://example.com",
                        "source_type", "medium",
                        "category_name", "Tech"
                ));

        when(filterPipeline.execute(any(TopicContext.class)))
                .thenReturn(new FilterPipeline.PipelineResult(false, List.of(
                        FilterResult.pass("duplicate-url"),
                        FilterResult.fail("negative-keyword", "Matched: hiring")
                )));

        handler.handle(job);

        verify(jdbcTemplate).update(contains("rejected"), contains("negative-keyword"), eq(42L));
    }

    @Test
    void handle_topicNotFound_skips() {
        PipelineJob job = new PipelineJob(Stage.ANALYZE, 999L);
        job.setId(1L);

        when(jdbcTemplate.queryForMap(contains("SELECT"), eq(999L)))
                .thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));

        handler.handle(job);

        verify(filterPipeline, never()).execute(any());
    }
}
