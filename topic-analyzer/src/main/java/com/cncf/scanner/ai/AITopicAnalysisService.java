package com.cncf.scanner.ai;

import com.cncf.scanner.model.Theme;
import com.cncf.scanner.service.ThemeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AITopicAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(AITopicAnalysisService.class);
    
    private final ChatClient chatClient;
    private final ThemeService themeService;
    
    @Value("${ai.enabled:true}")
    private boolean aiEnabled;
    
    @Autowired
    public AITopicAnalysisService(ChatClient chatClient, ThemeService themeService) {
        this.chatClient = chatClient;
        this.themeService = themeService;
    }
    
    /**
     * Analyze topic content using AI to extract key themes and insights
     */
    public TopicAnalysisResult analyzeTopic(String title, String content) {
        if (!aiEnabled) {
            logger.debug("AI analysis disabled, returning basic analysis");
            return createBasicAnalysis(title, content);
        }
        
        try {
            return performAIAnalysis(title, content);
        } catch (Exception e) {
            logger.error("AI analysis failed, falling back to basic analysis: {}", e.getMessage(), e);
            return createBasicAnalysis(title, content);
        }
    }
    
    private TopicAnalysisResult performAIAnalysis(String title, String content) {
        // Get available themes for context
        List<Theme> availableThemes = themeService.findAll();
        String themeNames = availableThemes.stream()
                .map(Theme::getName)
                .collect(Collectors.joining(", "));
        
        // Create AI prompt for topic analysis
        String promptTemplate = """
            Analyze the following cloud-native topic and provide insights:
            
            Title: {title}
            Content: {content}
            
            Available themes: {themes}
            
            Please provide:
            1. Primary theme (from available themes or suggest new one)
            2. Secondary themes (up to 2 additional themes)
            3. Key topics/keywords extracted
            4. Relevance score (0-1) for cloud-native content
            5. Brief summary (1-2 sentences)
            6. Technical complexity level (Beginner/Intermediate/Advanced)
            
            Respond in JSON format:
            {
                "primaryTheme": "theme_name",
                "secondaryThemes": ["theme1", "theme2"],
                "keywords": ["keyword1", "keyword2"],
                "relevanceScore": 0.8,
                "summary": "Brief summary",
                "complexityLevel": "Intermediate"
            }
            """;
        
        PromptTemplate template = new PromptTemplate(promptTemplate);
        Map<String, Object> variables = Map.of(
                "title", title != null ? title : "",
                "content", content != null ? content : "",
                "themes", themeNames
        );
        
        Prompt prompt = template.create(variables);
        ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
        
        String aiResponse = response.getResult().getOutput().getContent();
        logger.debug("AI analysis response: {}", aiResponse);
        
        return parseAIResponse(aiResponse, availableThemes);
    }
    
    private TopicAnalysisResult parseAIResponse(String aiResponse, List<Theme> availableThemes) {
        try {
            // Simple JSON parsing (in production, use proper JSON library)
            TopicAnalysisResult result = new TopicAnalysisResult();
            
            // Extract primary theme
            String primaryTheme = extractJsonValue(aiResponse, "primaryTheme");
            if (primaryTheme != null) {
                result.setPrimaryTheme(findOrCreateTheme(primaryTheme, availableThemes));
            }
            
            // Extract secondary themes
            String secondaryThemes = extractJsonValue(aiResponse, "secondaryThemes");
            if (secondaryThemes != null) {
                List<String> themeNames = parseStringArray(secondaryThemes);
                result.setSecondaryThemes(themeNames.stream()
                        .map(name -> findOrCreateTheme(name, availableThemes))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            }
            
            // Extract keywords
            String keywords = extractJsonValue(aiResponse, "keywords");
            if (keywords != null) {
                result.setKeywords(parseStringArray(keywords));
            }
            
            // Extract relevance score
            String relevanceScore = extractJsonValue(aiResponse, "relevanceScore");
            if (relevanceScore != null) {
                try {
                    result.setRelevanceScore(Double.parseDouble(relevanceScore));
                } catch (NumberFormatException e) {
                    result.setRelevanceScore(0.5);
                }
            }
            
            // Extract summary
            String summary = extractJsonValue(aiResponse, "summary");
            if (summary != null) {
                result.setSummary(summary);
            }
            
            // Extract complexity level
            String complexityLevel = extractJsonValue(aiResponse, "complexityLevel");
            if (complexityLevel != null) {
                result.setComplexityLevel(complexityLevel);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error parsing AI response: {}", e.getMessage(), e);
            return createBasicAnalysis(null, null);
        }
    }
    
    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            logger.debug("Error extracting JSON value for key {}: {}", key, e.getMessage());
        }
        return null;
    }
    
    private List<String> parseStringArray(String arrayString) {
        try {
            // Simple array parsing
            return Arrays.stream(arrayString.replaceAll("[\\[\\]\"]", "").split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.debug("Error parsing string array: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private Theme findOrCreateTheme(String themeName, List<Theme> availableThemes) {
        return availableThemes.stream()
                .filter(theme -> theme.getName().equalsIgnoreCase(themeName))
                .findFirst()
                .orElse(null); // In production, you might want to create new themes
    }
    
    private TopicAnalysisResult createBasicAnalysis(String title, String content) {
        TopicAnalysisResult result = new TopicAnalysisResult();
        result.setRelevanceScore(0.5);
        result.setSummary("Basic analysis - AI not available");
        result.setComplexityLevel("Unknown");
        result.setKeywords(extractBasicKeywords(title, content));
        return result;
    }
    
    private List<String> extractBasicKeywords(String title, String content) {
        List<String> keywords = new ArrayList<>();
        if (title != null) {
            keywords.addAll(Arrays.asList(title.toLowerCase().split("\\s+")));
        }
        if (content != null) {
            keywords.addAll(Arrays.asList(content.toLowerCase().split("\\s+")));
        }
        return keywords.stream()
                .filter(word -> word.length() > 3)
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }
    
    public static class TopicAnalysisResult {
        private Theme primaryTheme;
        private List<Theme> secondaryThemes = new ArrayList<>();
        private List<String> keywords = new ArrayList<>();
        private Double relevanceScore = 0.0;
        private String summary;
        private String complexityLevel;
        
        // Getters and Setters
        public Theme getPrimaryTheme() { return primaryTheme; }
        public void setPrimaryTheme(Theme primaryTheme) { this.primaryTheme = primaryTheme; }
        
        public List<Theme> getSecondaryThemes() { return secondaryThemes; }
        public void setSecondaryThemes(List<Theme> secondaryThemes) { this.secondaryThemes = secondaryThemes; }
        
        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { this.keywords = keywords; }
        
        public Double getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(Double relevanceScore) { this.relevanceScore = relevanceScore; }
        
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        
        public String getComplexityLevel() { return complexityLevel; }
        public void setComplexityLevel(String complexityLevel) { this.complexityLevel = complexityLevel; }
    }
}
