package com.topicscanner.api;

import com.topicscanner.queue.ScanOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineMonitorControllerTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private ScanOrchestrator scanOrchestrator;

    private PipelineMonitorController controller;

    @BeforeEach
    void setUp() {
        controller = new PipelineMonitorController(jdbcTemplate, scanOrchestrator);
    }

    @Test
    void getPipelineStatus_returnsJobCounts() {
        when(jdbcTemplate.queryForList(contains("GROUP BY stage, status")))
                .thenReturn(List.of(
                        Map.of("stage", "EXTRACT", "status", "PENDING", "count", 5L),
                        Map.of("stage", "ANALYZE", "status", "COMPLETED", "count", 100L)));

        when(jdbcTemplate.queryForObject(contains("PENDING"), eq(Long.class))).thenReturn(5L);
        when(jdbcTemplate.queryForObject(contains("PROCESSING"), eq(Long.class))).thenReturn(2L);
        when(jdbcTemplate.queryForObject(contains("COMPLETED"), eq(Long.class))).thenReturn(100L);
        when(jdbcTemplate.queryForObject(contains("FAILED"), eq(Long.class))).thenReturn(3L);

        Map<String, Object> status = controller.getPipelineStatus();

        assertNotNull(status.get("jobCounts"));
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) status.get("summary");
        assertEquals(5L, summary.get("pending"));
        assertEquals(100L, summary.get("completed"));
    }

    @Test
    void getRecentErrors_returnsFailedJobs() {
        when(jdbcTemplate.queryForList(contains("FAILED"), eq(20)))
                .thenReturn(List.of(Map.of(
                        "id", 1L, "stage", "EXTRACT",
                        "error_message", "Connection timeout")));

        List<Map<String, Object>> errors = controller.getRecentErrors(20);

        assertEquals(1, errors.size());
    }

    @Test
    void triggerScan_success_returns200() {
        doNothing().when(scanOrchestrator).scheduledScan();

        var response = controller.triggerScan();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue((Boolean) response.getBody().get("triggered"));
    }

    @Test
    void triggerScan_failure_returns500() {
        doThrow(new RuntimeException("DB down")).when(scanOrchestrator).scheduledScan();

        var response = controller.triggerScan();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("triggered"));
    }

    @Test
    void getTopicsByStage_returnsGroupedCounts() {
        when(jdbcTemplate.queryForList(contains("pipeline_stage")))
                .thenReturn(List.of(
                        Map.of("pipeline_stage", "analyzed", "count", 80),
                        Map.of("pipeline_stage", "scanned", "count", 20)));

        List<Map<String, Object>> result = controller.getTopicsByStage();

        assertEquals(2, result.size());
    }
}
