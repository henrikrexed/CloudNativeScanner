package com.topicscanner.scanner.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.topicscanner.scanner.ScanRequest;
import com.topicscanner.scanner.ScanResult;
import com.topicscanner.scanner.SourceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Scans YouTube for topics using the Google Data API v3.
 * Requires a YOUTUBE_API_KEY environment variable.
 */
@Component
public class YouTubeScanner implements SourceScanner {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeScanner.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3";

    private final WebClient webClient;
    private final String apiKey;

    public YouTubeScanner(WebClient.Builder webClientBuilder,
                          @Value("${YOUTUBE_API_KEY:}") String apiKey) {
        this.webClient = webClientBuilder.clone()
                .baseUrl(BASE_URL)
                .build();
        this.apiKey = apiKey;
    }

    @Override
    public String getSourceType() {
        return "youtube";
    }

    @Override
    public String getDisplayName() {
        return "YouTube";
    }

    @Override
    public List<ScanResult> scan(ScanRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("YouTube scan skipped: YOUTUBE_API_KEY not configured");
            return List.of();
        }

        String query = String.join(" ", request.keywords());
        if (query.isBlank()) {
            logger.warn("YouTube scan skipped: no keywords provided");
            return List.of();
        }

        try {
            // Search for videos
            String searchBody = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("part", "snippet")
                            .queryParam("q", query)
                            .queryParam("type", "video")
                            .queryParam("order", "relevance")
                            .queryParam("maxResults", Math.min(request.maxResults(), 25))
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (searchBody == null) {
                return List.of();
            }

            JsonNode searchRoot = MAPPER.readTree(searchBody);
            JsonNode items = searchRoot.path("items");

            if (!items.isArray() || items.isEmpty()) {
                return List.of();
            }

            // Collect video IDs for statistics lookup
            List<String> videoIds = StreamSupport.stream(items.spliterator(), false)
                    .map(item -> item.path("id").path("videoId").asText(""))
                    .filter(id -> !id.isBlank())
                    .toList();

            Map<String, JsonNode> statsMap = fetchVideoStats(videoIds);

            List<ScanResult> results = new ArrayList<>();
            for (JsonNode item : items) {
                String videoId = item.path("id").path("videoId").asText("");
                JsonNode snippet = item.path("snippet");
                String title = snippet.path("title").asText("");

                if (title.isBlank() || videoId.isBlank()) {
                    continue;
                }

                if (matchesNegativeKeywords(title, request.negativeKeywords())) {
                    continue;
                }

                String url = "https://www.youtube.com/watch?v=" + videoId;
                LocalDateTime sourceDate = parseDate(snippet.path("publishedAt").asText(""));

                Map<String, Object> metadata = buildMetadata(videoId, snippet, statsMap.get(videoId));
                results.add(new ScanResult(title, url, getSourceType(), metadata, sourceDate));
            }

            return results.stream().limit(request.maxResults()).toList();
        } catch (Exception e) {
            logger.warn("YouTube scan failed: {}", e.getMessage());
            return List.of();
        }
    }

    private Map<String, JsonNode> fetchVideoStats(List<String> videoIds) {
        if (videoIds.isEmpty()) {
            return Map.of();
        }

        try {
            String ids = String.join(",", videoIds);
            String body = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/videos")
                            .queryParam("part", "statistics,contentDetails")
                            .queryParam("id", ids)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (body == null) {
                return Map.of();
            }

            JsonNode root = MAPPER.readTree(body);
            return StreamSupport.stream(root.path("items").spliterator(), false)
                    .collect(Collectors.toMap(
                            item -> item.path("id").asText(),
                            item -> item,
                            (a, b) -> a
                    ));
        } catch (Exception e) {
            logger.warn("Failed to fetch YouTube video stats: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<String, Object> buildMetadata(String videoId, JsonNode snippet, JsonNode statsNode) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("externalId", videoId);
        metadata.put("channelTitle", snippet.path("channelTitle").asText(""));
        metadata.put("channelId", snippet.path("channelId").asText(""));

        if (statsNode != null) {
            JsonNode stats = statsNode.path("statistics");
            metadata.put("viewCount", stats.path("viewCount").asLong(0));
            metadata.put("likeCount", stats.path("likeCount").asLong(0));
            metadata.put("commentCount", stats.path("commentCount").asLong(0));

            String duration = statsNode.path("contentDetails").path("duration").asText("");
            metadata.put("duration", duration);
        }

        return metadata;
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private boolean matchesNegativeKeywords(String text, List<String> negativeKeywords) {
        String lower = text.toLowerCase();
        return negativeKeywords.stream()
                .anyMatch(kw -> lower.contains(kw.toLowerCase()));
    }
}
