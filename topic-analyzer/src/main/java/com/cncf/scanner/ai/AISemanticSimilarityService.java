package com.cncf.scanner.ai;

import com.cncf.scanner.model.Topic;
import com.cncf.scanner.service.TopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AISemanticSimilarityService {
    
    private static final Logger logger = LoggerFactory.getLogger(AISemanticSimilarityService.class);
    
    private final ChatClient chatClient;
    private final TopicService topicService;
    
    @Value("${ai.enabled:true}")
    private boolean aiEnabled;
    
    @Value("${ai.similarity.threshold:0.8}")
    private double similarityThreshold;
    
    @Autowired
    public AISemanticSimilarityService(ChatClient chatClient, TopicService topicService) {
        this.chatClient = chatClient;
        this.topicService = topicService;
    }
    
    /**
     * Check if a topic is semantically similar to existing topics
     */
    public SimilarityResult checkSimilarity(String title, String content) {
        if (!aiEnabled) {
            logger.debug("AI similarity check disabled");
            return new SimilarityResult(false, 0.0, null);
        }
        
        try {
            // Get recent topics for comparison
            List<Topic> recentTopics = topicService.findRecentTopics();
            if (recentTopics.isEmpty()) {
                return new SimilarityResult(false, 0.0, null);
            }
            
            // Limit to recent topics for performance
            List<Topic> topicsToCheck = recentTopics.stream()
                    .limit(50)
                    .toList();
            
            return performSimilarityCheck(title, content, topicsToCheck);
            
        } catch (Exception e) {
            logger.error("AI similarity check failed: {}", e.getMessage(), e);
            return new SimilarityResult(false, 0.0, null);
        }
    }
    
    private SimilarityResult performSimilarityCheck(String title, String content, List<Topic> existingTopics) {
        // Create a prompt for similarity analysis
        String promptTemplate = """
            Analyze the semantic similarity between the new topic and existing topics.
            
            New Topic:
            Title: {title}
            Content: {content}
            
            Existing Topics:
            {existingTopics}
            
            Determine if the new topic is semantically similar to any existing topic.
            Consider:
            - Similar technical concepts
            - Same problem domain
            - Overlapping solutions
            - Related technologies
            
            Respond with JSON:
            {
                "isSimilar": true/false,
                "similarityScore": 0.0-1.0,
                "mostSimilarTopicId": "topic_id_if_similar",
                "reason": "explanation"
            }
            """;
        
        PromptTemplate template = new PromptTemplate(promptTemplate);
        
        // Format existing topics for the prompt
        StringBuilder topicsText = new StringBuilder();
        for (Topic topic : existingTopics) {
            topicsText.append("ID: ").append(topic.getId())
                    .append(", Title: ").append(topic.getTitle())
                    .append(", Content: ").append(topic.getContent() != null ? 
                            topic.getContent().substring(0, Math.min(200, topic.getContent().length())) : "")
                    .append("\n");
        }
        
        Map<String, Object> variables = Map.of(
                "title", title != null ? title : "",
                "content", content != null ? content : "",
                "existingTopics", topicsText.toString()
        );
        
        Prompt prompt = template.create(variables);
        ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
        
        String aiResponse = response.getResult().getOutput().getContent();
        logger.debug("AI similarity response: {}", aiResponse);
        
        return parseSimilarityResponse(aiResponse);
    }
    
    private SimilarityResult parseSimilarityResponse(String aiResponse) {
        try {
            boolean isSimilar = extractBooleanValue(aiResponse, "isSimilar");
            double similarityScore = extractDoubleValue(aiResponse, "similarityScore");
            String mostSimilarTopicId = extractStringValue(aiResponse, "mostSimilarTopicId");
            String reason = extractStringValue(aiResponse, "reason");
            
            // Only consider it similar if score is above threshold
            if (isSimilar && similarityScore >= similarityThreshold) {
                return new SimilarityResult(true, similarityScore, mostSimilarTopicId, reason);
            } else {
                return new SimilarityResult(false, similarityScore, null, reason);
            }
            
        } catch (Exception e) {
            logger.error("Error parsing similarity response: {}", e.getMessage(), e);
            return new SimilarityResult(false, 0.0, null);
        }
    }
    
    private boolean extractBooleanValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*(true|false)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return Boolean.parseBoolean(m.group(1));
            }
        } catch (Exception e) {
            logger.debug("Error extracting boolean value for key {}: {}", key, e.getMessage());
        }
        return false;
    }
    
    private double extractDoubleValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*([0-9.]+)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return Double.parseDouble(m.group(1));
            }
        } catch (Exception e) {
            logger.debug("Error extracting double value for key {}: {}", key, e.getMessage());
        }
        return 0.0;
    }
    
    private String extractStringValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            logger.debug("Error extracting string value for key {}: {}", key, e.getMessage());
        }
        return null;
    }
    
    public static class SimilarityResult {
        private final boolean isSimilar;
        private final double similarityScore;
        private final String mostSimilarTopicId;
        private final String reason;
        
        public SimilarityResult(boolean isSimilar, double similarityScore, String mostSimilarTopicId) {
            this(isSimilar, similarityScore, mostSimilarTopicId, null);
        }
        
        public SimilarityResult(boolean isSimilar, double similarityScore, String mostSimilarTopicId, String reason) {
            this.isSimilar = isSimilar;
            this.similarityScore = similarityScore;
            this.mostSimilarTopicId = mostSimilarTopicId;
            this.reason = reason;
        }
        
        public boolean isSimilar() { return isSimilar; }
        public double getSimilarityScore() { return similarityScore; }
        public String getMostSimilarTopicId() { return mostSimilarTopicId; }
        public String getReason() { return reason; }
    }
}
