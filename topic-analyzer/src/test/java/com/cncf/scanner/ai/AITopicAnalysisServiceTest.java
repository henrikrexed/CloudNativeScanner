package com.cncf.scanner.ai;

import com.cncf.scanner.ai.AITopicAnalysisService.TopicAnalysisResult;
import com.cncf.scanner.model.Theme;
import com.cncf.scanner.service.ThemeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AITopicAnalysisServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ThemeService themeService;

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private Generation generation;

    @InjectMocks
    private AITopicAnalysisService aiTopicAnalysisService;

    private String testContent;
    private String testTitle;

    @BeforeEach
    void setUp() {
        testTitle = "How to deploy Kubernetes cluster?";
        testContent = "I'm trying to deploy a Kubernetes cluster on AWS using kops. " +
                "I've followed the documentation but I'm getting errors with the networking configuration. " +
                "Can someone help me with the proper setup?";
    }

    @Test
    void testAnalyzeTopicWithAIEnabled() {
        // Given
        List<Theme> mockThemes = Arrays.asList(
                new Theme(1L, "Kubernetes", "Container orchestration"),
                new Theme(2L, "AWS", "Amazon Web Services")
        );
        
        when(themeService.findAll()).thenReturn(mockThemes);
        when(chatClient.prompt(any(Prompt.class)).call().chatResponse()).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(new org.springframework.ai.chat.model.ChatResponse.Output() {
            @Override
            public String getContent() {
                return """
                    {
                        "primaryTheme": "Kubernetes",
                        "secondaryThemes": ["AWS"],
                        "keywords": ["kubernetes", "kops", "aws", "deployment"],
                        "relevanceScore": 0.9,
                        "summary": "User asking for help with Kubernetes cluster deployment on AWS using kops",
                        "complexityLevel": "Intermediate"
                    }
                    """;
            }
        });

        // When
        TopicAnalysisResult result = aiTopicAnalysisService.analyzeTopic(testTitle, testContent);

        // Then
        assertNotNull(result);
        assertNotNull(result.getPrimaryTheme());
        assertEquals("Kubernetes", result.getPrimaryTheme().getName());
        assertEquals(1, result.getSecondaryThemes().size());
        assertEquals("AWS", result.getSecondaryThemes().get(0).getName());
        assertEquals(4, result.getKeywords().size());
        assertTrue(result.getKeywords().contains("kubernetes"));
        assertEquals(0.9, result.getRelevanceScore(), 0.01);
        assertNotNull(result.getSummary());
        assertEquals("Intermediate", result.getComplexityLevel());
    }

    @Test
    void testAnalyzeTopicWithAIException() {
        // Given
        List<Theme> mockThemes = Arrays.asList(
                new Theme(1L, "Kubernetes", "Container orchestration")
        );
        
        when(themeService.findAll()).thenReturn(mockThemes);
        when(chatClient.prompt(any(Prompt.class)).call().chatResponse()).thenThrow(new RuntimeException("AI service error"));

        // When
        TopicAnalysisResult result = aiTopicAnalysisService.analyzeTopic(testTitle, testContent);

        // Then
        assertNotNull(result);
        // Should fall back to basic analysis
        assertNull(result.getPrimaryTheme());
        assertTrue(result.getSecondaryThemes().isEmpty());
        assertTrue(result.getKeywords().isEmpty());
        assertEquals(0.0, result.getRelevanceScore(), 0.01);
        assertNull(result.getSummary());
        assertNull(result.getComplexityLevel());
    }

    @Test
    void testAnalyzeTopicWithEmptyContent() {
        // When
        TopicAnalysisResult result = aiTopicAnalysisService.analyzeTopic("", "");

        // Then
        assertNotNull(result);
        assertNull(result.getPrimaryTheme());
        assertTrue(result.getSecondaryThemes().isEmpty());
        assertTrue(result.getKeywords().isEmpty());
        assertEquals(0.0, result.getRelevanceScore(), 0.01);
        assertNull(result.getSummary());
        assertNull(result.getComplexityLevel());
    }

    @Test
    void testAnalyzeTopicWithNullInputs() {
        // When
        TopicAnalysisResult result = aiTopicAnalysisService.analyzeTopic(null, null);

        // Then
        assertNotNull(result);
        assertNull(result.getPrimaryTheme());
        assertTrue(result.getSecondaryThemes().isEmpty());
        assertTrue(result.getKeywords().isEmpty());
        assertEquals(0.0, result.getRelevanceScore(), 0.01);
        assertNull(result.getSummary());
        assertNull(result.getComplexityLevel());
    }
}