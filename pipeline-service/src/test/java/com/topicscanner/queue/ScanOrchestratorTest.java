package com.topicscanner.queue;

import com.cncf.scanner.model.Category;
import com.cncf.scanner.model.PipelineJob;
import com.cncf.scanner.model.PipelineJob.Stage;
import com.cncf.scanner.service.CategoryService;
import com.topicscanner.scanner.*;
import com.topicscanner.telemetry.TelemetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScanOrchestratorTest {

    @Mock
    private CategoryService categoryService;
    @Mock
    private ScannerRegistry scannerRegistry;
    @Mock
    private PipelineOrchestrator pipelineOrchestrator;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private TelemetryService telemetryService;

    private ScanOrchestrator scanOrchestrator;

    static class StubRedditScanner implements SourceScanner {
        @Override
        public String getSourceType() {
            return "reddit";
        }

        @Override
        public String getDisplayName() {
            return "Reddit";
        }

        @Override
        public List<ScanResult> scan(ScanRequest request) {
            return List.of(
                    new ScanResult("K8s post", "https://reddit.com/r/k8s/1",
                            "reddit", Map.of("externalId", "abc123"), LocalDateTime.now()),
                    new ScanResult("Docker post", "https://reddit.com/r/docker/2",
                            "reddit", Map.of(), LocalDateTime.now())
            );
        }
    }

    @BeforeEach
    void setUp() {
        scanOrchestrator = new ScanOrchestrator(
                categoryService, scannerRegistry, pipelineOrchestrator, jdbcTemplate, 100, telemetryService);
    }

    private Category createTestCategory() {
        Category cat = new Category("Cloud Native", "Cloud native topics");
        cat.setId(1L);
        cat.setEnabled(true);
        cat.setKeywordsList(List.of("kubernetes", "docker"));
        cat.setNegativeKeywordsList(List.of("war"));
        cat.setSourcesList(List.of("reddit"));
        return cat;
    }

    @Test
    void scanCategory_newTopics_persistsAndEnqueues() {
        Category category = createTestCategory();

        when(scannerRegistry.getScanner("reddit"))
                .thenReturn(Optional.of(new StubRedditScanner()));
        when(jdbcTemplate.queryForList(contains("SELECT id FROM sources"), eq(Long.class), eq("reddit")))
                .thenReturn(List.of(1L));
        // First topic is new (INSERT RETURNING id returns the new id)
        when(jdbcTemplate.queryForList(contains("INSERT INTO topics"), eq(Long.class),
                eq(1L), eq(1L), eq("abc123"), anyString(), anyString(), any()))
                .thenReturn(List.of(100L));
        // Second topic is new
        when(jdbcTemplate.queryForList(contains("INSERT INTO topics"), eq(Long.class),
                eq(1L), eq(1L), eq("https://reddit.com/r/docker/2"), anyString(), anyString(), any()))
                .thenReturn(List.of(101L));

        when(pipelineOrchestrator.enqueue(any(), anyLong()))
                .thenReturn(new PipelineJob(Stage.EXTRACT, 100L));

        int result = scanOrchestrator.scanCategory(category);

        assertEquals(2, result);
        verify(pipelineOrchestrator).enqueue(Stage.EXTRACT, 100L);
        verify(pipelineOrchestrator).enqueue(Stage.EXTRACT, 101L);
    }

    @Test
    void scanCategory_duplicateTopics_skipped() {
        Category category = createTestCategory();

        when(scannerRegistry.getScanner("reddit"))
                .thenReturn(Optional.of(new StubRedditScanner()));
        when(jdbcTemplate.queryForList(contains("SELECT id FROM sources"), eq(Long.class), eq("reddit")))
                .thenReturn(List.of(1L));
        // Both topics already exist (INSERT ON CONFLICT returns empty list)
        when(jdbcTemplate.queryForList(contains("INSERT INTO topics"), eq(Long.class),
                anyLong(), anyLong(), anyString(), anyString(), anyString(), any()))
                .thenReturn(List.of());

        int result = scanOrchestrator.scanCategory(category);

        assertEquals(0, result);
        verify(pipelineOrchestrator, never()).enqueue(any(), anyLong());
    }

    @Test
    void scanCategory_noKeywords_returnsZero() {
        Category category = new Category("Empty", "No keywords");
        category.setId(2L);
        category.setSourcesList(List.of("reddit"));
        // keywords is null/empty

        int result = scanOrchestrator.scanCategory(category);

        assertEquals(0, result);
        verifyNoInteractions(scannerRegistry);
    }

    @Test
    void scanCategory_noMatchingScanner_skips() {
        Category category = createTestCategory();
        when(scannerRegistry.getScanner("reddit")).thenReturn(Optional.empty());

        int result = scanOrchestrator.scanCategory(category);

        assertEquals(0, result);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void scanCategory_scannerThrows_continuesWithOtherSources() {
        Category category = createTestCategory();
        category.setSourcesList(List.of("reddit", "stackoverflow"));

        SourceScanner failingScanner = new SourceScanner() {
            @Override
            public String getSourceType() {
                return "reddit";
            }

            @Override
            public String getDisplayName() {
                return "Reddit";
            }

            @Override
            public List<ScanResult> scan(ScanRequest request) {
                throw new RuntimeException("API rate limited");
            }
        };

        when(scannerRegistry.getScanner("reddit")).thenReturn(Optional.of(failingScanner));
        when(scannerRegistry.getScanner("stackoverflow")).thenReturn(Optional.empty());

        // Should not throw
        int result = scanOrchestrator.scanCategory(category);

        assertEquals(0, result);
    }

    @Test
    void scheduledScan_callsFindEnabled() {
        when(categoryService.findEnabled()).thenReturn(List.of());

        scanOrchestrator.scheduledScan();

        verify(categoryService).findEnabled();
    }
}
