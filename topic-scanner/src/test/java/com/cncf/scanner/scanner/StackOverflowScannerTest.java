package com.cncf.scanner.scanner;

import com.cncf.scanner.model.Source;
import com.cncf.scanner.model.SearchTopic;
import com.cncf.scanner.service.SearchTopicService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StackOverflowScannerTest {

    @Mock
    private SearchTopicService searchTopicService;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private StackOverflowScanner stackOverflowScanner;

    private Source testSource;
    private SearchTopic testSearchTopic;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        testSource = new Source();
        testSource.setId(1L);
        testSource.setName("StackOverflow");
        testSource.setBaseUrl("https://stackoverflow.com");
        testSource.setIsActive(true);

        testSearchTopic = new SearchTopic();
        testSearchTopic.setId(1L);
        testSearchTopic.setSource(testSource);
        testSearchTopic.setKeyword("kubernetes");
        testSearchTopic.setSearchQuery("kubernetes deployment");
        testSearchTopic.setDescription("Kubernetes questions");
        testSearchTopic.setIsActive(true);
        testSearchTopic.setPriority(1);
        testSearchTopic.setMaxResults(50);

        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetSourceType() {
        assertEquals("StackOverflow", stackOverflowScanner.getSourceType());
    }

    @Test
    void testCanHandle() {
        assertTrue(stackOverflowScanner.canHandle(testSource));
        
        Source otherSource = new Source();
        otherSource.setName("Reddit");
        assertFalse(stackOverflowScanner.canHandle(otherSource));
    }

    @Test
    void testScanWithNoSearchTopics() {
        // Given
        when(searchTopicService.findSearchTopicsNeedingSearch(1L)).thenReturn(Arrays.asList());

        // When
        List<ScanResult> results = stackOverflowScanner.scan(testSource, LocalDateTime.now().minusDays(1));

        // Then
        assertTrue(results.isEmpty());
        verify(searchTopicService).findSearchTopicsNeedingSearch(1L);
    }

    @Test
    void testScanWithSearchTopics() throws Exception {
        // Given
        List<SearchTopic> searchTopics = Arrays.asList(testSearchTopic);
        when(searchTopicService.findSearchTopicsNeedingSearch(1L)).thenReturn(searchTopics);
        
        // Mock WebClient chain
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        // Mock JSON response
        String jsonResponse = """
            {
                "items": [
                    {
                        "question_id": 12345,
                        "title": "How to deploy Kubernetes?",
                        "link": "https://stackoverflow.com/questions/12345",
                        "owner": {
                            "display_name": "testuser"
                        },
                        "score": 10,
                        "view_count": 100,
                        "answer_count": 5,
                        "creation_date": 1640995200,
                        "body": "<p>How do I deploy a Kubernetes cluster?</p>",
                        "tags": ["kubernetes", "deployment"],
                        "is_answered": true,
                        "accepted_answer_id": 67890
                    }
                ]
            }
            """;
        
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(jsonResponse));

        // When
        List<ScanResult> results = stackOverflowScanner.scan(testSource, LocalDateTime.now().minusDays(1));

        // Then
        assertEquals(1, results.size());
        ScanResult result = results.get(0);
        assertEquals("12345", result.getExternalId());
        assertEquals("How to deploy Kubernetes?", result.getTitle());
        assertEquals("https://stackoverflow.com/questions/12345", result.getUrl());
        assertEquals("testuser", result.getAuthor());
        assertEquals(10, result.getScore());
        assertEquals(100, result.getViewCount());
        assertEquals(5, result.getInteractionCount());
        assertNotNull(result.getContent());
        assertNotNull(result.getMetadata());
        
        verify(searchTopicService).updateLastSearchedAt(1L);
    }

    @Test
    void testScanWithEmptyResponse() {
        // Given
        List<SearchTopic> searchTopics = Arrays.asList(testSearchTopic);
        when(searchTopicService.findSearchTopicsNeedingSearch(1L)).thenReturn(searchTopics);
        
        // Mock WebClient chain
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("{\"items\": []}"));

        // When
        List<ScanResult> results = stackOverflowScanner.scan(testSource, LocalDateTime.now().minusDays(1));

        // Then
        assertTrue(results.isEmpty());
        verify(searchTopicService).updateLastSearchedAt(1L);
    }

    @Test
    void testScanWithNullResponse() {
        // Given
        List<SearchTopic> searchTopics = Arrays.asList(testSearchTopic);
        when(searchTopicService.findSearchTopicsNeedingSearch(1L)).thenReturn(searchTopics);
        
        // Mock WebClient chain
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(""));

        // When
        List<ScanResult> results = stackOverflowScanner.scan(testSource, LocalDateTime.now().minusDays(1));

        // Then
        assertTrue(results.isEmpty());
        verify(searchTopicService).updateLastSearchedAt(1L);
    }

    @Test
    void testGetRateLimit() {
        assertEquals(30, stackOverflowScanner.getRateLimit());
    }

    @Test
    void testGetRequestDelay() {
        assertEquals(2000, stackOverflowScanner.getRequestDelay());
    }

    @Test
    void testScanWithException() {
        // Given
        List<SearchTopic> searchTopics = Arrays.asList(testSearchTopic);
        when(searchTopicService.findSearchTopicsNeedingSearch(1L)).thenReturn(searchTopics);
        when(webClient.get()).thenThrow(new RuntimeException("Network error"));

        // When
        List<ScanResult> results = stackOverflowScanner.scan(testSource, LocalDateTime.now().minusDays(1));

        // Then
        assertTrue(results.isEmpty());
        verify(searchTopicService, never()).updateLastSearchedAt(anyLong());
    }
}
