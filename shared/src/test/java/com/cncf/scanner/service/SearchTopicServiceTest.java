package com.cncf.scanner.service;

import com.cncf.scanner.model.SearchTopic;
import com.cncf.scanner.model.Source;
import com.cncf.scanner.repository.SearchTopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchTopicServiceTest {

    @Mock
    private SearchTopicRepository searchTopicRepository;

    @InjectMocks
    private SearchTopicService searchTopicService;

    private Source testSource;
    private SearchTopic testSearchTopic;

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
        testSearchTopic.setDescription("Kubernetes-related questions");
        testSearchTopic.setIsActive(true);
        testSearchTopic.setPriority(1);
        testSearchTopic.setMaxResults(50);
        testSearchTopic.setSearchFrequencyHours(24);
    }

    @Test
    void testFindAll() {
        // Given
        List<SearchTopic> expectedTopics = Arrays.asList(testSearchTopic);
        when(searchTopicRepository.findAll()).thenReturn(expectedTopics);

        // When
        List<SearchTopic> result = searchTopicService.findAll();

        // Then
        assertEquals(1, result.size());
        assertEquals(testSearchTopic, result.get(0));
        verify(searchTopicRepository).findAll();
    }

    @Test
    void testFindBySourceId() {
        // Given
        Long sourceId = 1L;
        List<SearchTopic> expectedTopics = Arrays.asList(testSearchTopic);
        when(searchTopicRepository.findBySourceId(sourceId)).thenReturn(expectedTopics);

        // When
        List<SearchTopic> result = searchTopicService.findBySourceId(sourceId);

        // Then
        assertEquals(1, result.size());
        assertEquals(testSearchTopic, result.get(0));
        verify(searchTopicRepository).findBySourceId(sourceId);
    }

    @Test
    void testFindActiveBySourceId() {
        // Given
        Long sourceId = 1L;
        List<SearchTopic> expectedTopics = Arrays.asList(testSearchTopic);
        when(searchTopicRepository.findBySourceIdAndIsActiveTrue(sourceId)).thenReturn(expectedTopics);

        // When
        List<SearchTopic> result = searchTopicService.findActiveBySourceId(sourceId);

        // Then
        assertEquals(1, result.size());
        assertEquals(testSearchTopic, result.get(0));
        verify(searchTopicRepository).findBySourceIdAndIsActiveTrue(sourceId);
    }

    @Test
    void testFindSearchTopicsNeedingSearch() {
        // Given
        Long sourceId = 1L;
        List<SearchTopic> expectedTopics = Arrays.asList(testSearchTopic);
        when(searchTopicRepository.findSearchTopicsNeedingSearch(eq(sourceId), any(LocalDateTime.class)))
                .thenReturn(expectedTopics);

        // When
        List<SearchTopic> result = searchTopicService.findSearchTopicsNeedingSearch(sourceId);

        // Then
        assertEquals(1, result.size());
        assertEquals(testSearchTopic, result.get(0));
        verify(searchTopicRepository).findSearchTopicsNeedingSearch(eq(sourceId), any(LocalDateTime.class));
    }

    @Test
    void testFindById() {
        // Given
        Long id = 1L;
        when(searchTopicRepository.findById(id)).thenReturn(Optional.of(testSearchTopic));

        // When
        Optional<SearchTopic> result = searchTopicService.findById(id);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testSearchTopic, result.get());
        verify(searchTopicRepository).findById(id);
    }

    @Test
    void testFindByIdNotFound() {
        // Given
        Long id = 999L;
        when(searchTopicRepository.findById(id)).thenReturn(Optional.empty());

        // When
        Optional<SearchTopic> result = searchTopicService.findById(id);

        // Then
        assertFalse(result.isPresent());
        verify(searchTopicRepository).findById(id);
    }

    @Test
    void testSave() {
        // Given
        when(searchTopicRepository.save(testSearchTopic)).thenReturn(testSearchTopic);

        // When
        SearchTopic result = searchTopicService.save(testSearchTopic);

        // Then
        assertEquals(testSearchTopic, result);
        verify(searchTopicRepository).save(testSearchTopic);
    }

    @Test
    void testCreateSearchTopic() {
        // Given
        when(searchTopicRepository.save(any(SearchTopic.class))).thenReturn(testSearchTopic);

        // When
        SearchTopic result = searchTopicService.createSearchTopic(
                testSource, "kubernetes", "kubernetes deployment", 
                "Kubernetes questions", 1, 50);

        // Then
        assertNotNull(result);
        verify(searchTopicRepository).save(any(SearchTopic.class));
    }

    @Test
    void testUpdateSearchTopic() {
        // Given
        when(searchTopicRepository.findById(1L)).thenReturn(Optional.of(testSearchTopic));
        when(searchTopicRepository.save(any(SearchTopic.class))).thenReturn(testSearchTopic);

        // When
        SearchTopic result = searchTopicService.updateSearchTopic(
                1L, "docker", "docker containers", "Docker questions", 
                true, 2, 30, 12);

        // Then
        assertNotNull(result);
        verify(searchTopicRepository).findById(1L);
        verify(searchTopicRepository).save(any(SearchTopic.class));
    }

    @Test
    void testUpdateSearchTopicNotFound() {
        // Given
        when(searchTopicRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            searchTopicService.updateSearchTopic(999L, "docker", "docker", "Docker", true, 1, 50, 24);
        });
        verify(searchTopicRepository).findById(999L);
        verify(searchTopicRepository, never()).save(any(SearchTopic.class));
    }

    @Test
    void testToggleSearchTopicStatus() {
        // Given
        when(searchTopicRepository.findById(1L)).thenReturn(Optional.of(testSearchTopic));
        when(searchTopicRepository.save(any(SearchTopic.class))).thenReturn(testSearchTopic);

        // When
        SearchTopic result = searchTopicService.toggleSearchTopicStatus(1L);

        // Then
        assertNotNull(result);
        verify(searchTopicRepository).findById(1L);
        verify(searchTopicRepository).save(any(SearchTopic.class));
    }

    @Test
    void testUpdateLastSearchedAt() {
        // Given
        when(searchTopicRepository.findById(1L)).thenReturn(Optional.of(testSearchTopic));
        when(searchTopicRepository.save(any(SearchTopic.class))).thenReturn(testSearchTopic);

        // When
        searchTopicService.updateLastSearchedAt(1L);

        // Then
        verify(searchTopicRepository).findById(1L);
        verify(searchTopicRepository).save(any(SearchTopic.class));
    }

    @Test
    void testDelete() {
        // Given
        Long id = 1L;

        // When
        searchTopicService.delete(id);

        // Then
        verify(searchTopicRepository).deleteById(id);
    }

    @Test
    void testGetSearchTopicStats() {
        // Given
        Long sourceId = 1L;
        when(searchTopicRepository.countBySourceId(sourceId)).thenReturn(5L);
        when(searchTopicRepository.countBySourceIdAndIsActiveTrue(sourceId)).thenReturn(3L);

        // When
        SearchTopicService.SearchTopicStats stats = searchTopicService.getSearchTopicStats(sourceId);

        // Then
        assertEquals(5L, stats.getTotalTopics());
        assertEquals(3L, stats.getActiveTopics());
        verify(searchTopicRepository).countBySourceId(sourceId);
        verify(searchTopicRepository).countBySourceIdAndIsActiveTrue(sourceId);
    }
}


