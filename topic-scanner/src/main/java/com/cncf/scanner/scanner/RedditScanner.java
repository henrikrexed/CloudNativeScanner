package com.cncf.scanner.scanner;

import com.cncf.scanner.config.DebugConfig;
import com.cncf.scanner.model.Source;
import com.cncf.scanner.model.SearchTopic;
import com.cncf.scanner.service.SearchTopicService;
import com.cncf.scanner.util.ScanTracer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class RedditScanner implements SourceScanner {
    
    private static final Logger logger = LoggerFactory.getLogger(RedditScanner.class);
    private static final String SOURCE_TYPE = "Reddit";
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final SearchTopicService searchTopicService;
    private final DebugConfig debugConfig;
    
    @Autowired
    public RedditScanner(SearchTopicService searchTopicService, DebugConfig debugConfig) {
        this.webClient = WebClient.builder()
                .baseUrl("https://www.reddit.com")
                .defaultHeader("User-Agent", "CloudNativeScanner/1.0")
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
        ScanTracer.startTrace("REDDIT_SCAN", source.getName(), 
                "Starting Reddit scan for source: " + source.getName());
        Instant scanStart = Instant.now();
        List<ScanResult> results = new ArrayList<>();
        
        try {
            // Get configurable search topics for this source
            ScanTracer.logStep("REDDIT_SCAN", source.getName(), "GETTING_SEARCH_TOPICS", 
                    "Fetching search topics for source ID: " + source.getId());
            
            List<SearchTopic> searchTopics = searchTopicService.findSearchTopicsNeedingSearch(source.getId());
            
            if (searchTopics.isEmpty()) {
                ScanTracer.logStep("REDDIT_SCAN", source.getName(), "NO_TOPICS", 
                        "No search topics configured for this source");
                logger.info("📭 No search topics configured for source: {}", source.getName());
                return results;
            }
            
            ScanTracer.logStep("REDDIT_SCAN", source.getName(), "TOPICS_FOUND", 
                    "Found " + searchTopics.size() + " search topics to process");
            logger.info("🔍 Scanning {} search topics for Reddit source: {}", searchTopics.size(), source.getName());
            
            Map<String, Object> scanMetrics = new HashMap<>();
            scanMetrics.put("totalTopics", searchTopics.size());
            scanMetrics.put("successfulTopics", 0);
            scanMetrics.put("failedTopics", 0);
            scanMetrics.put("totalResults", 0);
            
            for (SearchTopic searchTopic : searchTopics) {
                try {
                    ScanTracer.logStep("REDDIT_SCAN", source.getName(), "PROCESSING_TOPIC", 
                            "Processing topic: " + searchTopic.getKeyword() + " (ID: " + searchTopic.getId() + ")");
                    
                    List<ScanResult> topicResults = searchReddit(searchTopic, lastScanTime);
                    results.addAll(topicResults);
                    
                    scanMetrics.put("successfulTopics", (Integer) scanMetrics.get("successfulTopics") + 1);
                    scanMetrics.put("totalResults", (Integer) scanMetrics.get("totalResults") + topicResults.size());
                    
                    // Update last searched timestamp
                    searchTopicService.updateLastSearchedAt(searchTopic.getId());
                    
                    ScanTracer.logDataCollection(source.getName(), searchTopic.getKeyword(), 
                            topicResults.size(), "Reddit search completed");
                    
                    logger.debug("✅ Found {} results for search topic: {}", topicResults.size(), searchTopic.getKeyword());
                    
                    if (debugConfig.isDetailedScanLogging() && !topicResults.isEmpty()) {
                        logger.debug("📋 Topic '{}' results: {}", searchTopic.getKeyword(), 
                                topicResults.stream().map(r -> r.getTitle()).toArray());
                    }
                    
                } catch (Exception e) {
                    scanMetrics.put("failedTopics", (Integer) scanMetrics.get("failedTopics") + 1);
                    ScanTracer.logError("REDDIT_SCAN", source.getName(), 
                            "Failed to search topic: " + searchTopic.getKeyword(), e);
                    logger.error("❌ Error searching for topic {}: {}", searchTopic.getKeyword(), e.getMessage(), e);
                }
                
                // Rate limiting
                long delay = getRequestDelay();
                ScanTracer.logStep("REDDIT_SCAN", source.getName(), "RATE_LIMITING", 
                        "Sleeping for " + delay + "ms between requests");
                Thread.sleep(delay);
            }
            
            Duration scanDuration = Duration.between(scanStart, Instant.now());
            scanMetrics.put("scanDurationMs", scanDuration.toMillis());
            scanMetrics.put("avgResultsPerTopic", searchTopics.size() > 0 ? 
                    (Integer) scanMetrics.get("totalResults") / searchTopics.size() : 0);
            
            ScanTracer.logMetrics("REDDIT_SCAN", source.getName(), scanMetrics);
            
            logger.info("🎯 Reddit scan completed for {}: {} topics processed, {} results found in {}ms", 
                    source.getName(), searchTopics.size(), results.size(), scanDuration.toMillis());
            
        } catch (Exception e) {
            Duration scanDuration = Duration.between(scanStart, Instant.now());
            ScanTracer.logError("REDDIT_SCAN", source.getName(), 
                    "Reddit scan failed after " + scanDuration.toMillis() + "ms", e);
            logger.error("💥 Error scanning Reddit after {}ms: {}", scanDuration.toMillis(), e.getMessage(), e);
        } finally {
            ScanTracer.endTrace("REDDIT_SCAN", source.getName(), 
                    "Reddit scan completed with " + results.size() + " results");
        }
        
        return results;
    }
    
    private List<ScanResult> searchReddit(SearchTopic searchTopic, LocalDateTime lastScanTime) {
        List<ScanResult> results = new ArrayList<>();
        
        try {
            // Reddit search can be done in multiple ways:
            // 1. Search specific subreddits
            // 2. Search across all of Reddit
            // 3. Browse subreddit by keyword
            
            String searchQuery = searchTopic.getSearchQuery() != null ? 
                    searchTopic.getSearchQuery() : searchTopic.getKeyword();
            
            // For now, we'll search in relevant subreddits
            String[] subreddits = getRelevantSubreddits(searchQuery);
            
            for (String subreddit : subreddits) {
                try {
                    List<ScanResult> subredditResults = searchSubreddit(subreddit, searchQuery, searchTopic, lastScanTime);
                    results.addAll(subredditResults);
                    
                    Thread.sleep(1000); // Rate limiting between subreddits
                    
                } catch (Exception e) {
                    logger.error("Error searching subreddit {}: {}", subreddit, e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error searching Reddit for topic {}: {}", searchTopic.getKeyword(), e.getMessage(), e);
        }
        
        return results;
    }
    
    private List<ScanResult> searchSubreddit(String subreddit, String searchQuery, SearchTopic searchTopic, LocalDateTime lastScanTime) {
        List<ScanResult> results = new ArrayList<>();
        
        try {
            // Search in subreddit
            String searchUrl = String.format("/r/%s/search.json?q=%s&sort=new&limit=%d", 
                    subreddit, searchQuery, Math.min(searchTopic.getMaxResults(), 25));
            
            String response = webClient.get()
                    .uri(searchUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode data = root.get("data");
                JsonNode children = data.get("children");
                
                if (children != null && children.isArray()) {
                    for (JsonNode child : children) {
                        JsonNode postData = child.get("data");
                        ScanResult result = parseRedditPost(postData, subreddit, searchTopic);
                        if (result != null && isNewOrUpdated(result, lastScanTime)) {
                            results.add(result);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error searching subreddit {}: {}", subreddit, e.getMessage(), e);
        }
        
        return results;
    }
    
    private String[] getRelevantSubreddits(String searchQuery) {
        // Map search queries to relevant subreddits
        String query = searchQuery.toLowerCase();
        
        if (query.contains("kubernetes") || query.contains("k8s")) {
            return new String[]{"kubernetes", "k8s", "devops"};
        } else if (query.contains("docker")) {
            return new String[]{"docker", "devops", "containers"};
        } else if (query.contains("microservices")) {
            return new String[]{"microservices", "devops", "programming"};
        } else if (query.contains("cloud") || query.contains("native")) {
            return new String[]{"cloudnative", "devops", "aws", "gcp", "azure"};
        } else if (query.contains("devops")) {
            return new String[]{"devops", "sysadmin", "kubernetes"};
        } else {
            // Default subreddits for general cloud-native topics
            return new String[]{"devops", "kubernetes", "docker", "programming"};
        }
    }
    
    private ScanResult parseRedditPost(JsonNode postData, String subreddit, SearchTopic searchTopic) {
        try {
            ScanResult result = new ScanResult();
            
            result.setExternalId(postData.get("id").asText());
            result.setTitle(postData.get("title").asText());
            result.setUrl("https://reddit.com" + postData.get("permalink").asText());
            result.setAuthor(postData.get("author").asText());
            result.setScore(postData.get("score").asInt());
            result.setViewCount(postData.get("num_comments").asInt());
            result.setInteractionCount(postData.get("num_comments").asInt());
            
            // Parse creation date
            double creationDate = postData.get("created_utc").asDouble();
            result.setPublishedAt(LocalDateTime.ofEpochSecond((long) creationDate, 0, 
                    java.time.ZoneOffset.UTC));
            
            // Get post content
            String selftext = postData.get("selftext").asText();
            if (selftext != null && !selftext.isEmpty() && !"[deleted]".equals(selftext)) {
                result.setContent(selftext);
            } else {
                // For link posts, use the title as content
                result.setContent(postData.get("title").asText());
            }
            
            // Add metadata including search topic information
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("subreddit", subreddit);
            metadata.put("subreddit_id", postData.get("subreddit_id").asText());
            metadata.put("is_self", postData.get("is_self").asBoolean());
            metadata.put("domain", postData.get("domain").asText());
            metadata.put("url", postData.get("url").asText());
            metadata.put("over_18", postData.get("over_18").asBoolean());
            metadata.put("spoiler", postData.get("spoiler").asBoolean());
            metadata.put("search_topic_id", searchTopic.getId());
            metadata.put("search_topic_keyword", searchTopic.getKeyword());
            metadata.put("search_topic_priority", searchTopic.getPriority());
            result.setMetadata(metadata);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error parsing Reddit post: {}", e.getMessage(), e);
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
        return 60; // Reddit allows 60 requests per minute
    }
    
    @Override
    public long getRequestDelay() {
        return 1000; // 1 second between requests
    }
}

