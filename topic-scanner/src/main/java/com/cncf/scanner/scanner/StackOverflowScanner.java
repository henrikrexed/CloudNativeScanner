package com.cncf.scanner.scanner;

import com.cncf.scanner.config.DebugConfig;
import com.cncf.scanner.model.Source;
import com.cncf.scanner.model.SearchTopic;
import com.cncf.scanner.service.SearchTopicService;
import com.cncf.scanner.util.ScanTracer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class StackOverflowScanner implements SourceScanner {
    
    private static final Logger logger = LoggerFactory.getLogger(StackOverflowScanner.class);
    private static final String SOURCE_TYPE = "StackOverflow";
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final SearchTopicService searchTopicService;
    private final DebugConfig debugConfig;
    
    @Autowired
    public StackOverflowScanner(SearchTopicService searchTopicService, DebugConfig debugConfig) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.stackexchange.com/2.3")
                .build();
        this.objectMapper = new ObjectMapper();
        this.searchTopicService = searchTopicService;
        this.debugConfig = debugConfig;
    }
    
    @Override
    public String getSourceType() {
        return SOURCE_TYPE;
    }
    
    @Override
    public boolean canHandle(Source source) {
        return SOURCE_TYPE.equalsIgnoreCase(source.getName());
    }
    
    @Override
    public List<ScanResult> scan(Source source, LocalDateTime lastScanTime) {
        ScanTracer.startTrace("STACKOVERFLOW_SCAN", source.getName(), 
                "Starting StackOverflow scan for source: " + source.getName());
        Instant scanStart = Instant.now();
        List<ScanResult> results = new ArrayList<>();
        
        try {
            // Get configurable search topics for this source
            ScanTracer.logStep("STACKOVERFLOW_SCAN", source.getName(), "GETTING_SEARCH_TOPICS", 
                    "Fetching search topics for source ID: " + source.getId());
            
            List<SearchTopic> searchTopics = searchTopicService.findSearchTopicsNeedingSearch(source.getId());
            
            if (searchTopics.isEmpty()) {
                ScanTracer.logStep("STACKOVERFLOW_SCAN", source.getName(), "NO_TOPICS", 
                        "No search topics configured for this source");
                logger.info("📭 No search topics configured for source: {}", source.getName());
                return results;
            }
            
            ScanTracer.logStep("STACKOVERFLOW_SCAN", source.getName(), "TOPICS_FOUND", 
                    "Found " + searchTopics.size() + " search topics to process");
            logger.info("🔍 Scanning {} search topics for StackOverflow source: {}", searchTopics.size(), source.getName());
            
            Map<String, Object> scanMetrics = new HashMap<>();
            scanMetrics.put("totalTopics", searchTopics.size());
            scanMetrics.put("successfulTopics", 0);
            scanMetrics.put("failedTopics", 0);
            scanMetrics.put("totalResults", 0);
            
            for (SearchTopic searchTopic : searchTopics) {
                try {
                    ScanTracer.logStep("STACKOVERFLOW_SCAN", source.getName(), "PROCESSING_TOPIC", 
                            "Processing topic: " + searchTopic.getKeyword() + " (ID: " + searchTopic.getId() + ")");
                    
                    List<ScanResult> topicResults = searchStackOverflow(searchTopic, lastScanTime);
                    results.addAll(topicResults);
                    
                    scanMetrics.put("successfulTopics", (Integer) scanMetrics.get("successfulTopics") + 1);
                    scanMetrics.put("totalResults", (Integer) scanMetrics.get("totalResults") + topicResults.size());
                    
                    // Update last searched timestamp
                    searchTopicService.updateLastSearchedAt(searchTopic.getId());
                    
                    ScanTracer.logDataCollection(source.getName(), searchTopic.getKeyword(), 
                            topicResults.size(), "StackOverflow search completed");
                    
                    logger.debug("✅ Found {} results for search topic: {}", topicResults.size(), searchTopic.getKeyword());
                    
                    if (debugConfig.isDetailedScanLogging() && !topicResults.isEmpty()) {
                        logger.debug("📋 Topic '{}' results: {}", searchTopic.getKeyword(), 
                                topicResults.stream().map(r -> r.getTitle()).toArray());
                    }
                    
                } catch (Exception e) {
                    scanMetrics.put("failedTopics", (Integer) scanMetrics.get("failedTopics") + 1);
                    ScanTracer.logError("STACKOVERFLOW_SCAN", source.getName(), 
                            "Failed to search topic: " + searchTopic.getKeyword(), e);
                    logger.error("❌ Error searching for topic {}: {}", searchTopic.getKeyword(), e.getMessage(), e);
                }
                
                // Rate limiting
                long delay = getRequestDelay();
                ScanTracer.logStep("STACKOVERFLOW_SCAN", source.getName(), "RATE_LIMITING", 
                        "Sleeping for " + delay + "ms between requests");
                Thread.sleep(delay);
            }
            
            Duration scanDuration = Duration.between(scanStart, Instant.now());
            scanMetrics.put("scanDurationMs", scanDuration.toMillis());
            scanMetrics.put("avgResultsPerTopic", searchTopics.size() > 0 ? 
                    (Integer) scanMetrics.get("totalResults") / searchTopics.size() : 0);
            
            ScanTracer.logMetrics("STACKOVERFLOW_SCAN", source.getName(), scanMetrics);
            
            logger.info("🎯 StackOverflow scan completed for {}: {} topics processed, {} results found in {}ms", 
                    source.getName(), searchTopics.size(), results.size(), scanDuration.toMillis());
            
        } catch (Exception e) {
            Duration scanDuration = Duration.between(scanStart, Instant.now());
            ScanTracer.logError("STACKOVERFLOW_SCAN", source.getName(), 
                    "StackOverflow scan failed after " + scanDuration.toMillis() + "ms", e);
            logger.error("💥 Error scanning StackOverflow after {}ms: {}", scanDuration.toMillis(), e.getMessage(), e);
        } finally {
            ScanTracer.endTrace("STACKOVERFLOW_SCAN", source.getName(), 
                    "StackOverflow scan completed with " + results.size() + " results");
        }
        
        return results;
    }
    
    private List<ScanResult> searchStackOverflow(SearchTopic searchTopic, LocalDateTime lastScanTime) {
        List<ScanResult> results = new ArrayList<>();
        
        try {
            // Build search URL with configurable parameters
            String searchQuery = searchTopic.getSearchQuery() != null ? 
                    searchTopic.getSearchQuery() : searchTopic.getKeyword();
            
            String url = String.format("/search/advanced?order=desc&sort=activity&q=%s&site=stackoverflow&pagesize=%d", 
                    searchQuery, searchTopic.getMaxResults());
            
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode items = root.get("items");
                
                if (items != null && items.isArray()) {
                    for (JsonNode item : items) {
                        ScanResult result = parseStackOverflowItem(item, searchTopic);
                        if (result != null && isNewOrUpdated(result, lastScanTime)) {
                            results.add(result);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error searching StackOverflow for topic {}: {}", searchTopic.getKeyword(), e.getMessage(), e);
        }
        
        return results;
    }
    
    private ScanResult parseStackOverflowItem(JsonNode item, SearchTopic searchTopic) {
        try {
            ScanResult result = new ScanResult();
            
            result.setExternalId(item.get("question_id").asText());
            result.setTitle(item.get("title").asText());
            result.setUrl(item.get("link").asText());
            
            // Get owner information
            JsonNode owner = item.get("owner");
            if (owner != null && !owner.isNull()) {
                result.setAuthor(owner.get("display_name").asText());
            }
            
            result.setScore(item.get("score").asInt());
            result.setViewCount(item.get("view_count").asInt());
            result.setInteractionCount(item.get("answer_count").asInt());
            
            // Parse creation date
            long creationDate = item.get("creation_date").asLong();
            result.setPublishedAt(LocalDateTime.ofEpochSecond(creationDate, 0, 
                    java.time.ZoneOffset.UTC));
            
            // Get question body (content)
            String body = item.get("body").asText();
            Document doc = Jsoup.parse(body);
            result.setContent(doc.text());
            
            // Add metadata including search topic information
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("tags", item.get("tags"));
            metadata.put("is_answered", item.get("is_answered").asBoolean());
            metadata.put("accepted_answer_id", item.get("accepted_answer_id"));
            metadata.put("search_topic_id", searchTopic.getId());
            metadata.put("search_topic_keyword", searchTopic.getKeyword());
            metadata.put("search_topic_priority", searchTopic.getPriority());
            result.setMetadata(metadata);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error parsing StackOverflow item: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private boolean isNewOrUpdated(ScanResult result, LocalDateTime lastScanTime) {
        if (lastScanTime == null) {
            return true;
        }
        return result.getPublishedAt().isAfter(lastScanTime);
    }
    
    @Override
    public int getRateLimit() {
        return 30; // StackOverflow allows 30 requests per second
    }
    
    @Override
    public long getRequestDelay() {
        return 2000; // 2 seconds between requests to be safe
    }
}

