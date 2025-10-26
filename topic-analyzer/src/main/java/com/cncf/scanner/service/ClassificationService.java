package com.cncf.scanner.service;

import com.cncf.scanner.model.Theme;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClassificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ClassificationService.class);
    
    private final ThemeService themeService;
    
    // Keyword mappings for theme classification
    private final Map<String, List<String>> themeKeywords = Map.of(
            "Kubernetes", Arrays.asList("kubernetes", "k8s", "pod", "deployment", "service", "ingress", "helm", "operator"),
            "Cloud Native", Arrays.asList("cloud-native", "cloud native", "microservices", "container", "docker", "orchestration"),
            "DevOps", Arrays.asList("devops", "ci/cd", "pipeline", "jenkins", "gitlab", "github actions", "automation"),
            "Security", Arrays.asList("security", "vulnerability", "cve", "rbac", "network policy", "secrets", "encryption"),
            "Monitoring", Arrays.asList("monitoring", "observability", "prometheus", "grafana", "jaeger", "logging", "metrics"),
            "Development", Arrays.asList("programming", "code", "api", "rest", "graphql", "testing", "debugging"),
            "Architecture", Arrays.asList("architecture", "design pattern", "scalability", "performance", "load balancing"),
            "Performance", Arrays.asList("performance", "optimization", "benchmark", "latency", "throughput", "memory", "cpu")
    );
    
    @Autowired
    public ClassificationService(ThemeService themeService) {
        this.themeService = themeService;
    }
    
    /**
     * Classify content into themes based on keywords and content analysis
     */
    public List<ThemeClassification> classifyContent(String title, String content) {
        List<ThemeClassification> classifications = new ArrayList<>();
        
        if (StringUtils.isBlank(title) && StringUtils.isBlank(content)) {
            return classifications;
        }
        
        String fullText = (title + " " + content).toLowerCase();
        
        // Get all themes from database
        List<Theme> allThemes = themeService.findAll();
        
        for (Theme theme : allThemes) {
            Double confidenceScore = calculateConfidenceScore(theme.getName(), fullText);
            
            // Only include themes with confidence score above threshold
            if (confidenceScore > 0.1) {
                classifications.add(new ThemeClassification(theme, confidenceScore));
            }
        }
        
        // Sort by confidence score (highest first) and limit to top 3
        return classifications.stream()
                .sorted((a, b) -> Double.compare(b.getConfidenceScore(), a.getConfidenceScore()))
                .limit(3)
                .collect(Collectors.toList());
    }
    
    private Double calculateConfidenceScore(String themeName, String text) {
        List<String> keywords = themeKeywords.get(themeName);
        if (keywords == null || keywords.isEmpty()) {
            return 0.0;
        }
        
        int keywordMatches = 0;
        int totalKeywords = keywords.size();
        
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                keywordMatches++;
            }
        }
        
        // Calculate confidence based on keyword matches
        double baseScore = (double) keywordMatches / totalKeywords;
        
        // Boost score for title matches (titles are more important)
        if (text.toLowerCase().contains(themeName.toLowerCase())) {
            baseScore += 0.2;
        }
        
        // Normalize to 0-1 range
        return Math.min(1.0, baseScore);
    }
    
    /**
     * Enhanced classification using AI/ML (placeholder for future implementation)
     */
    public List<ThemeClassification> classifyContentWithAI(String title, String content) {
        // This is a placeholder for future AI-based classification
        // Could integrate with OpenAI, Hugging Face, or other ML services
        logger.debug("AI classification not yet implemented, falling back to keyword-based classification");
        return classifyContent(title, content);
    }
}
