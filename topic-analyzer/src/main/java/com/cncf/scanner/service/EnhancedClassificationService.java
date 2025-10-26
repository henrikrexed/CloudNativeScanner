package com.cncf.scanner.service;

import com.cncf.scanner.ai.AISemanticSimilarityService;
import com.cncf.scanner.ai.AITopicAnalysisService;
import com.cncf.scanner.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EnhancedClassificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedClassificationService.class);
    
    private final AITopicAnalysisService aiTopicAnalysisService;
    private final AISemanticSimilarityService aiSimilarityService;
    private final TopicService topicService;
    private final ThemeService themeService;
    private final TopicAnalysisService topicAnalysisService;
    
    @Value("${ai.enabled:true}")
    private boolean aiEnabled;
    
    @Value("${ai.duplicate_detection.enabled:true}")
    private boolean duplicateDetectionEnabled;
    
    @Autowired
    public EnhancedClassificationService(AITopicAnalysisService aiTopicAnalysisService,
                                       AISemanticSimilarityService aiSimilarityService,
                                       TopicService topicService,
                                       ThemeService themeService,
                                       TopicAnalysisService topicAnalysisService) {
        this.aiTopicAnalysisService = aiTopicAnalysisService;
        this.aiSimilarityService = aiSimilarityService;
        this.topicService = topicService;
        this.themeService = themeService;
        this.topicAnalysisService = topicAnalysisService;
    }
    
    /**
     * Enhanced classification using AI for better topic understanding
     */
    public ClassificationResult classifyTopic(String title, String content) {
        logger.info("Starting enhanced classification for topic: {}", title);
        
        ClassificationResult result = new ClassificationResult();
        
        try {
            // Step 1: Check for semantic similarity (duplicate detection)
            if (duplicateDetectionEnabled && aiEnabled) {
                AISemanticSimilarityService.SimilarityResult similarityResult = 
                        aiSimilarityService.checkSimilarity(title, content);
                
                if (similarityResult.isSimilar()) {
                    logger.info("Topic is similar to existing topic: {}", similarityResult.getMostSimilarTopicId());
                    result.setDuplicate(true);
                    result.setSimilarityScore(similarityResult.getSimilarityScore());
                    result.setSimilarTopicId(similarityResult.getMostSimilarTopicId());
                    result.setReason("Semantic similarity detected: " + similarityResult.getReason());
                    return result;
                }
            }
            
            // Step 2: Perform AI topic analysis
            AITopicAnalysisService.TopicAnalysisResult analysisResult = null;
            if (aiEnabled) {
                analysisResult = aiTopicAnalysisService.analyzeTopic(title, content);
                result.setAiAnalysis(analysisResult);
            }
            
            // Step 3: Determine themes based on AI analysis
            List<ThemeClassification> themeClassifications = determineThemes(analysisResult, title, content);
            result.setThemeClassifications(themeClassifications);
            
            // Step 4: Calculate overall relevance
            double relevanceScore = calculateRelevanceScore(analysisResult, themeClassifications);
            result.setRelevanceScore(relevanceScore);
            
            // Step 5: Determine if topic should be processed
            boolean shouldProcess = shouldProcessTopic(relevanceScore, themeClassifications);
            result.setShouldProcess(shouldProcess);
            
            logger.info("Classification completed - Relevance: {}, Themes: {}, Process: {}", 
                    relevanceScore, themeClassifications.size(), shouldProcess);
            
        } catch (Exception e) {
            logger.error("Error in enhanced classification: {}", e.getMessage(), e);
            result.setError(e.getMessage());
            result.setShouldProcess(false);
        }
        
        return result;
    }
    
    private List<ThemeClassification> determineThemes(AITopicAnalysisService.TopicAnalysisResult analysisResult, 
                                                    String title, String content) {
        List<ThemeClassification> classifications = new ArrayList<>();
        
        if (analysisResult != null) {
            // Use AI-determined themes
            if (analysisResult.getPrimaryTheme() != null) {
                classifications.add(new ThemeClassification(
                        analysisResult.getPrimaryTheme(), 
                        analysisResult.getRelevanceScore() != null ? analysisResult.getRelevanceScore() : 0.8
                ));
            }
            
            if (analysisResult.getSecondaryThemes() != null) {
                for (Theme theme : analysisResult.getSecondaryThemes()) {
                    classifications.add(new ThemeClassification(theme, 0.6));
                }
            }
        }
        
        // Fallback to keyword-based classification if AI didn't provide themes
        if (classifications.isEmpty()) {
            classifications = fallbackKeywordClassification(title, content);
        }
        
        return classifications;
    }
    
    private List<ThemeClassification> fallbackKeywordClassification(String title, String content) {
        // This would use the original keyword-based classification
        // For now, return empty list - the original ClassificationService can handle this
        return new ArrayList<>();
    }
    
    private double calculateRelevanceScore(AITopicAnalysisService.TopicAnalysisResult analysisResult, 
                                         List<ThemeClassification> themeClassifications) {
        if (analysisResult != null && analysisResult.getRelevanceScore() != null) {
            return analysisResult.getRelevanceScore();
        }
        
        // Calculate based on theme classifications
        if (!themeClassifications.isEmpty()) {
            return themeClassifications.stream()
                    .mapToDouble(ThemeClassification::getConfidenceScore)
                    .average()
                    .orElse(0.0);
        }
        
        return 0.5; // Default relevance
    }
    
    private boolean shouldProcessTopic(double relevanceScore, List<ThemeClassification> themeClassifications) {
        // Process if relevance is above threshold and has at least one theme
        return relevanceScore >= 0.3 && !themeClassifications.isEmpty();
    }
    
    /**
     * Save AI analysis results to database
     */
    public void saveAIAnalysis(Topic topic, AITopicAnalysisService.TopicAnalysisResult analysisResult) {
        if (analysisResult == null) {
            return;
        }
        
        try {
            TopicAnalysis analysis = new TopicAnalysis(topic);
            analysis.setAiSummary(analysisResult.getSummary());
            analysis.setComplexityLevel(analysisResult.getComplexityLevel());
            analysis.setRelevanceScore(analysisResult.getRelevanceScore());
            analysis.setKeywords(String.join(",", analysisResult.getKeywords()));
            analysis.setAiConfidence(analysisResult.getRelevanceScore());
            
            topicAnalysisService.save(analysis);
            
            logger.debug("Saved AI analysis for topic: {}", topic.getTitle());
            
        } catch (Exception e) {
            logger.error("Error saving AI analysis: {}", e.getMessage(), e);
        }
    }
    
    public static class ClassificationResult {
        private boolean duplicate = false;
        private double similarityScore = 0.0;
        private String similarTopicId;
        private String reason;
        private AITopicAnalysisService.TopicAnalysisResult aiAnalysis;
        private List<ThemeClassification> themeClassifications = new ArrayList<>();
        private double relevanceScore = 0.0;
        private boolean shouldProcess = true;
        private String error;
        
        // Getters and Setters
        public boolean isDuplicate() { return duplicate; }
        public void setDuplicate(boolean duplicate) { this.duplicate = duplicate; }
        
        public double getSimilarityScore() { return similarityScore; }
        public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }
        
        public String getSimilarTopicId() { return similarTopicId; }
        public void setSimilarTopicId(String similarTopicId) { this.similarTopicId = similarTopicId; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public AITopicAnalysisService.TopicAnalysisResult getAiAnalysis() { return aiAnalysis; }
        public void setAiAnalysis(AITopicAnalysisService.TopicAnalysisResult aiAnalysis) { this.aiAnalysis = aiAnalysis; }
        
        public List<ThemeClassification> getThemeClassifications() { return themeClassifications; }
        public void setThemeClassifications(List<ThemeClassification> themeClassifications) { this.themeClassifications = themeClassifications; }
        
        public double getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }
        
        public boolean isShouldProcess() { return shouldProcess; }
        public void setShouldProcess(boolean shouldProcess) { this.shouldProcess = shouldProcess; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
