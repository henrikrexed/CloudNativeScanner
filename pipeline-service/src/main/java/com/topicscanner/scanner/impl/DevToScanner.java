package com.topicscanner.scanner.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.topicscanner.scanner.ScanRequest;
import com.topicscanner.scanner.ScanResult;
import com.topicscanner.scanner.ScannerUtils;
import com.topicscanner.scanner.SourceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Scans Dev.to for topics using the Forem API.
 */
@Component
public class DevToScanner implements SourceScanner {

    private static final Logger logger = LoggerFactory.getLogger(DevToScanner.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String BASE_URL = "https://dev.to/api";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final long REQUEST_DELAY_MS = 1000;

    private final WebClient webClient;

    public DevToScanner(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.clone()
                .baseUrl(BASE_URL)
                .defaultHeader("Accept", "application/vnd.forem.api-v1+json")
                .defaultHeader("User-Agent", "TopicScanner/2.0 (DevRel Intelligence)")
                .build();
    }

    @Override
    public String getSourceType() {
        return "devto";
    }

    @Override
    public String getDisplayName() {
        return "Dev.to";
    }

    @Override
    public List<ScanResult> scan(ScanRequest request) {
        List<ScanResult> results = new ArrayList<>();

        if (request.keywords().isEmpty()) {
            logger.warn("Dev.to scan skipped: no keywords provided");
            return results;
        }

        for (String keyword : request.keywords()) {
            if (results.size() >= request.maxResults()) {
                break;
            }
            try {
                String tag = keyword.toLowerCase().replaceAll("[^a-z0-9]", "");
                if (tag.isBlank()) continue;

                List<ScanResult> batch = searchByTag(tag, request);
                results.addAll(batch);

                if (request.keywords().size() > 1) {
                    Thread.sleep(REQUEST_DELAY_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.warn("Dev.to scan failed for keyword '{}': {}", keyword, e.getMessage());
            }
        }

        return results.stream().limit(request.maxResults()).toList();
    }

    private List<ScanResult> searchByTag(String tag, ScanRequest request) {
        String body = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/articles")
                        .queryParam("tag", tag)
                        .queryParam("per_page", Math.min(request.maxResults(), 30))
                        .queryParam("state", "rising")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block(REQUEST_TIMEOUT);

        if (body == null) return List.of();

        List<ScanResult> results = new ArrayList<>();
        try {
            JsonNode articles = MAPPER.readTree(body);
            for (JsonNode article : articles) {
                String title = article.path("title").asText("");
                String url = article.path("url").asText("");

                if (title.isBlank() || url.isBlank()) continue;
                if (ScannerUtils.matchesNegativeKeywords(title, request.negativeKeywords())) continue;

                var sourceDate = ScannerUtils.parseIsoDate(article.path("published_at").asText(""));

                List<String> tags = new ArrayList<>();
                for (JsonNode tagNode : article.path("tag_list")) {
                    tags.add(tagNode.asText());
                }

                results.add(new ScanResult(title, url, getSourceType(),
                        Map.of(
                                "externalId", String.valueOf(article.path("id").asLong()),
                                "author", article.path("user").path("username").asText(""),
                                "readingTimeMinutes", article.path("reading_time_minutes").asInt(0),
                                "positiveReactionsCount", article.path("positive_reactions_count").asInt(0),
                                "commentsCount", article.path("comments_count").asInt(0),
                                "tags", tags
                        ),
                        sourceDate));
            }
        } catch (Exception e) {
            logger.warn("Failed to parse Dev.to response: {}", e.getMessage());
        }
        return results;
    }
}
