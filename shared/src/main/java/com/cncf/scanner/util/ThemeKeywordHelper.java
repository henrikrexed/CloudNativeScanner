package com.cncf.scanner.util;

import com.cncf.scanner.model.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Helper utility to generate better search keywords from theme names
 * when themes don't have keywords configured.
 * This provides a smarter fallback than just using the theme name.
 */
public class ThemeKeywordHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(ThemeKeywordHelper.class);
    
    /**
     * Generate search keywords from theme name when keywords are missing
     * This creates better search terms than just using the theme name
     */
    public static List<String> generateKeywordsFromThemeName(Theme theme) {
        List<String> keywords = new ArrayList<>();
        String themeName = theme.getName().toLowerCase().trim();
        
        // Always include the theme name itself
        keywords.add(themeName);
        
        // Generate variations based on common patterns
        Map<String, List<String>> themeVariations = getThemeVariations();
        
        // Check if we have predefined variations for this theme
        boolean foundVariations = false;
        for (Map.Entry<String, List<String>> entry : themeVariations.entrySet()) {
            if (themeName.contains(entry.getKey()) || entry.getKey().equals(themeName)) {
                keywords.addAll(entry.getValue());
                foundVariations = true;
                break;
            }
        }
        
        // If no predefined variations, generate smart variations
        if (!foundVariations) {
            keywords.addAll(generateSmartVariations(themeName));
        }
        
        // Remove duplicates and limit to 7 keywords
        Set<String> uniqueKeywords = new LinkedHashSet<>();
        for (String keyword : keywords) {
            if (uniqueKeywords.size() < 7 && keyword != null && !keyword.trim().isEmpty()) {
                uniqueKeywords.add(keyword.toLowerCase().trim());
            }
        }
        
        List<String> result = new ArrayList<>(uniqueKeywords);
        logger.info("Generated {} fallback keywords for theme '{}' (no keywords configured): {}", 
                result.size(), theme.getName(), result);
        
        return result;
    }
    
    /**
     * Get predefined variations for common themes
     */
    private static Map<String, List<String>> getThemeVariations() {
        Map<String, List<String>> variations = new HashMap<>();
        
        // Kubernetes variations
        variations.put("kubernetes", Arrays.asList(
            "k8s", "container orchestration", "kubectl", "kubernetes tutorial", 
            "pods", "helm charts", "deployments"
        ));
        
        // ServiceMesh variations
        variations.put("servicemesh", Arrays.asList(
            "service mesh", "istio", "linkerd", "consul connect", 
            "envoy proxy", "microservices networking", "service mesh architecture"
        ));
        variations.put("service mesh", Arrays.asList(
            "servicemesh", "istio", "linkerd", "consul connect", 
            "envoy proxy", "microservices networking", "service mesh architecture"
        ));
        
        // Observability variations
        variations.put("observability", Arrays.asList(
            "monitoring", "tracing", "logging", "metrics", 
            "apm", "distributed tracing", "observability tools"
        ));
        
        // AI variations
        variations.put("ai", Arrays.asList(
            "artificial intelligence", "machine learning", "ml", "ai tools", 
            "ai applications", "ai development", "generative ai"
        ));
        variations.put("artificial intelligence", Arrays.asList(
            "ai", "machine learning", "ml", "ai tools", 
            "ai applications", "ai development", "generative ai"
        ));
        
        // Dynatrace variations
        variations.put("dynatrace", Arrays.asList(
            "dynatrace monitoring", "dynatrace apm", "dynatrace oneagent", 
            "dynatrace kubernetes", "dynatrace observability", "dynatrace platform"
        ));
        
        // Security variations
        variations.put("security", Arrays.asList(
            "cybersecurity", "cloud security", "kubernetes security", 
            "container security", "devsecops", "security best practices"
        ));
        
        // OpenTelemetry variations
        variations.put("opentelemetry", Arrays.asList(
            "otel", "open telemetry", "distributed tracing", "observability", 
            "opentelemetry collector", "opentelemetry instrumentation", "otel sdk"
        ));
        variations.put("open telemetry", Arrays.asList(
            "opentelemetry", "otel", "distributed tracing", "observability", 
            "opentelemetry collector", "opentelemetry instrumentation", "otel sdk"
        ));
        
        // Docker variations
        variations.put("docker", Arrays.asList(
            "containers", "containerization", "docker compose", 
            "dockerfile", "container images", "docker swarm"
        ));
        
        // Microservices variations
        variations.put("microservices", Arrays.asList(
            "microservices architecture", "microservices patterns", 
            "distributed systems", "service mesh", "api gateway"
        ));
        
        return variations;
    }
    
    /**
     * Generate smart variations from theme name when no predefined variations exist
     */
    private static List<String> generateSmartVariations(String themeName) {
        List<String> variations = new ArrayList<>();
        
        // Split camelCase or PascalCase
        if (themeName.matches(".*[a-z][A-Z].*")) {
            // Split camelCase: "ServiceMesh" -> "service", "mesh"
            String[] parts = themeName.split("(?=[A-Z])");
            if (parts.length > 1) {
                variations.add(String.join(" ", parts).toLowerCase());
            }
        }
        
        // Split on common separators
        if (themeName.contains("-") || themeName.contains("_")) {
            String[] parts = themeName.split("[-_]");
            for (String part : parts) {
                if (part.length() > 2) {
                    variations.add(part);
                }
            }
        }
        
        // Add common suffixes/prefixes
        if (themeName.length() > 3) {
            variations.add(themeName + " tutorial");
            variations.add(themeName + " guide");
            variations.add(themeName + " best practices");
        }
        
        return variations;
    }
}
