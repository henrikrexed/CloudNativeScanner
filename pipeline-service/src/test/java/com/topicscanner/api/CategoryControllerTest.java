package com.topicscanner.api;

import com.topicscanner.scanner.ScannerRegistry;
import com.topicscanner.scanner.SourceScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private ScannerRegistry scannerRegistry;

    private CategoryController controller;

    @BeforeEach
    void setUp() {
        controller = new CategoryController(jdbcTemplate, scannerRegistry);
    }

    @Test
    void listCategories_returnsAllWithCounts() {
        List<Map<String, Object>> expected = List.of(
                Map.of("id", 1L, "name", "Cloud Native", "topic_count", 50));
        when(jdbcTemplate.queryForList(contains("FROM categories")))
                .thenReturn(expected);

        List<Map<String, Object>> result = controller.listCategories();

        assertEquals(1, result.size());
        assertEquals("Cloud Native", result.get(0).get("name"));
    }

    @Test
    void createCategory_valid_returns201() {
        when(jdbcTemplate.queryForList(contains("INSERT INTO categories"), eq(Long.class),
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(5L));

        var response = controller.createCategory(Map.of("name", "DevOps"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(5L, response.getBody().get("id"));
    }

    @Test
    void createCategory_noName_returns400() {
        var response = controller.createCategory(Map.of("description", "no name"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void updateCategory_existing_returns200() {
        when(jdbcTemplate.update(contains("UPDATE categories"), any(), any(), any(), any(), any(),
                any(), any(), eq(1L)))
                .thenReturn(1);

        var response = controller.updateCategory(1L, Map.of("name", "Updated Name"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteCategory_existing_returns204() {
        when(jdbcTemplate.update("DELETE FROM categories WHERE id = ?", 1L))
                .thenReturn(1);

        var response = controller.deleteCategory(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void listScanners_returnsRegisteredScanners() {
        SourceScanner mockScanner = mock(SourceScanner.class);
        when(mockScanner.getSourceType()).thenReturn("medium");
        when(mockScanner.getDisplayName()).thenReturn("Medium");

        Collection<SourceScanner> scanners = List.of(mockScanner);
        when(scannerRegistry.getAllScanners()).thenReturn(scanners);

        List<Map<String, String>> result = controller.listScanners();

        assertEquals(1, result.size());
        assertEquals("medium", result.get(0).get("type"));
        assertEquals("Medium", result.get(0).get("displayName"));
    }
}
