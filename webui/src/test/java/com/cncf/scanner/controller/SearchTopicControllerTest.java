package com.cncf.scanner.controller;

import com.cncf.scanner.model.SearchTopic;
import com.cncf.scanner.model.Source;
import com.cncf.scanner.service.SearchTopicService;
import com.cncf.scanner.service.SourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SearchTopicControllerTest {

    @Mock
    private SearchTopicService searchTopicService;

    @Mock
    private SourceService sourceService;

    @Mock
    private Model model;

    @InjectMocks
    private SearchTopicController searchTopicController;

    private MockMvc mockMvc;
    private Source testSource;
    private SearchTopic testSearchTopic;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(searchTopicController).build();

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
    }

    @Test
    void testSearchTopics() {
        // Given
        List<SearchTopic> searchTopics = Arrays.asList(testSearchTopic);
        List<Source> sources = Arrays.asList(testSource);
        when(searchTopicService.findAll()).thenReturn(searchTopics);
        when(sourceService.findAll()).thenReturn(sources);

        // When
        String viewName = searchTopicController.searchTopics(null, model);

        // Then
        assertEquals("admin/search-topics", viewName);
        verify(searchTopicService).findAll();
        verify(sourceService).findAll();
        verify(model).addAttribute("searchTopics", searchTopics);
        verify(model).addAttribute("sources", sources);
    }

    @Test
    void testSearchTopicsWithSourceId() {
        // Given
        Long sourceId = 1L;
        List<SearchTopic> searchTopics = Arrays.asList(testSearchTopic);
        List<Source> sources = Arrays.asList(testSource);
        when(searchTopicService.findBySourceId(sourceId)).thenReturn(searchTopics);
        when(sourceService.findAll()).thenReturn(sources);

        // When
        String viewName = searchTopicController.searchTopics(sourceId, model);

        // Then
        assertEquals("admin/search-topics", viewName);
        verify(searchTopicService).findBySourceId(sourceId);
        verify(sourceService).findAll();
        verify(model).addAttribute("searchTopics", searchTopics);
        verify(model).addAttribute("sources", sources);
        verify(model).addAttribute("selectedSourceId", sourceId);
    }

    @Test
    void testNewSearchTopicForm() {
        // Given
        Long sourceId = 1L;
        List<Source> sources = Arrays.asList(testSource);
        when(sourceService.findById(sourceId)).thenReturn(Optional.of(testSource));
        when(sourceService.findAll()).thenReturn(sources);

        // When
        String viewName = searchTopicController.newSearchTopicForm(sourceId, model);

        // Then
        assertEquals("admin/search-topic-form", viewName);
        verify(sourceService).findById(sourceId);
        verify(sourceService).findAll();
        verify(model).addAttribute(eq("searchTopic"), any(SearchTopic.class));
        verify(model).addAttribute("sources", sources);
    }

    @Test
    void testNewSearchTopicFormWithoutSourceId() {
        // Given
        List<Source> sources = Arrays.asList(testSource);
        when(sourceService.findAll()).thenReturn(sources);

        // When
        String viewName = searchTopicController.newSearchTopicForm(null, model);

        // Then
        assertEquals("admin/search-topic-form", viewName);
        verify(sourceService, never()).findById(anyLong());
        verify(sourceService).findAll();
        verify(model).addAttribute(eq("searchTopic"), any(SearchTopic.class));
        verify(model).addAttribute("sources", sources);
    }

    @Test
    void testEditSearchTopicForm() {
        // Given
        Long id = 1L;
        List<Source> sources = Arrays.asList(testSource);
        when(searchTopicService.findById(id)).thenReturn(Optional.of(testSearchTopic));
        when(sourceService.findAll()).thenReturn(sources);

        // When
        String viewName = searchTopicController.editSearchTopicForm(id, model);

        // Then
        assertEquals("admin/search-topic-form", viewName);
        verify(searchTopicService).findById(id);
        verify(sourceService).findAll();
        verify(model).addAttribute("searchTopic", testSearchTopic);
        verify(model).addAttribute("sources", sources);
    }

    @Test
    void testEditSearchTopicFormNotFound() {
        // Given
        Long id = 999L;
        when(searchTopicService.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            searchTopicController.editSearchTopicForm(id, model);
        });
        verify(searchTopicService).findById(id);
        verify(sourceService, never()).findAll();
    }

    @Test
    void testSaveSearchTopicNew() {
        // Given
        SearchTopic newSearchTopic = new SearchTopic();
        newSearchTopic.setSource(testSource);
        newSearchTopic.setKeyword("docker");
        newSearchTopic.setSearchQuery("docker containers");
        newSearchTopic.setDescription("Docker questions");
        newSearchTopic.setPriority(1);
        newSearchTopic.setMaxResults(50);

        when(searchTopicService.createSearchTopic(any(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(testSearchTopic);

        // When
        String redirectUrl = searchTopicController.saveSearchTopic(newSearchTopic, null);

        // Then
        assertEquals("redirect:/admin/search-topics", redirectUrl);
        verify(searchTopicService).createSearchTopic(any(), anyString(), anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void testSaveSearchTopicUpdate() {
        // Given
        testSearchTopic.setKeyword("updated keyword");
        when(searchTopicService.updateSearchTopic(anyLong(), anyString(), anyString(), anyString(), 
                anyBoolean(), anyInt(), anyInt(), anyInt())).thenReturn(testSearchTopic);

        // When
        String redirectUrl = searchTopicController.saveSearchTopic(testSearchTopic, null);

        // Then
        assertEquals("redirect:/admin/search-topics", redirectUrl);
        verify(searchTopicService).updateSearchTopic(anyLong(), anyString(), anyString(), anyString(), 
                anyBoolean(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void testSaveSearchTopicWithException() {
        // Given
        SearchTopic newSearchTopic = new SearchTopic();
        newSearchTopic.setSource(testSource);
        newSearchTopic.setKeyword("docker");

        when(searchTopicService.createSearchTopic(any(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Database error"));

        // When
        String redirectUrl = searchTopicController.saveSearchTopic(newSearchTopic, null);

        // Then
        assertEquals("redirect:/admin/search-topics", redirectUrl);
        verify(searchTopicService).createSearchTopic(any(), anyString(), anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void testToggleSearchTopic() {
        // Given
        Long id = 1L;
        when(searchTopicService.toggleSearchTopicStatus(id)).thenReturn(testSearchTopic);

        // When
        String redirectUrl = searchTopicController.toggleSearchTopic(id, null);

        // Then
        assertEquals("redirect:/admin/search-topics", redirectUrl);
        verify(searchTopicService).toggleSearchTopicStatus(id);
    }

    @Test
    void testToggleSearchTopicWithException() {
        // Given
        Long id = 1L;
        when(searchTopicService.toggleSearchTopicStatus(id)).thenThrow(new RuntimeException("Database error"));

        // When
        String redirectUrl = searchTopicController.toggleSearchTopic(id, null);

        // Then
        assertEquals("redirect:/admin/search-topics", redirectUrl);
        verify(searchTopicService).toggleSearchTopicStatus(id);
    }

    @Test
    void testDeleteSearchTopic() {
        // Given
        Long id = 1L;
        doNothing().when(searchTopicService).delete(id);

        // When
        String redirectUrl = searchTopicController.deleteSearchTopic(id, null);

        // Then
        assertEquals("redirect:/admin/search-topics", redirectUrl);
        verify(searchTopicService).delete(id);
    }

    @Test
    void testDeleteSearchTopicWithException() {
        // Given
        Long id = 1L;
        doThrow(new RuntimeException("Database error")).when(searchTopicService).delete(id);

        // When
        String redirectUrl = searchTopicController.deleteSearchTopic(id, null);

        // Then
        assertEquals("redirect:/admin/search-topics", redirectUrl);
        verify(searchTopicService).delete(id);
    }

    @Test
    void testSearchTopicsBySource() {
        // Given
        Long sourceId = 1L;
        List<SearchTopic> searchTopics = Arrays.asList(testSearchTopic);
        when(searchTopicService.findBySourceId(sourceId)).thenReturn(searchTopics);
        when(sourceService.findById(sourceId)).thenReturn(Optional.of(testSource));

        // When
        String viewName = searchTopicController.searchTopicsBySource(sourceId, model);

        // Then
        assertEquals("admin/search-topics-by-source", viewName);
        verify(searchTopicService).findBySourceId(sourceId);
        verify(sourceService).findById(sourceId);
        verify(model).addAttribute("searchTopics", searchTopics);
        verify(model).addAttribute("source", testSource);
    }

    @Test
    void testSearchTopicsBySourceNotFound() {
        // Given
        Long sourceId = 999L;
        when(sourceService.findById(sourceId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            searchTopicController.searchTopicsBySource(sourceId, model);
        });
        verify(sourceService).findById(sourceId);
        verify(searchTopicService, never()).findBySourceId(anyLong());
    }

    @Test
    void testSearchTopicsMvc() throws Exception {
        // Given
        List<SearchTopic> searchTopics = Arrays.asList(testSearchTopic);
        List<Source> sources = Arrays.asList(testSource);
        when(searchTopicService.findAll()).thenReturn(searchTopics);
        when(sourceService.findAll()).thenReturn(sources);

        // When & Then
        mockMvc.perform(get("/admin/search-topics"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/search-topics"));
    }

    @Test
    void testNewSearchTopicFormMvc() throws Exception {
        // Given
        List<Source> sources = Arrays.asList(testSource);
        when(sourceService.findAll()).thenReturn(sources);

        // When & Then
        mockMvc.perform(get("/admin/search-topics/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/search-topic-form"));
    }
}


