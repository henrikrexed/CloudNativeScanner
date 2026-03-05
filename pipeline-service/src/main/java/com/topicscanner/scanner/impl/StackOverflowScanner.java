package com.topicscanner.scanner.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.topicscanner.scanner.ScanRequest;
import com.topicscanner.scanner.ScanResult;
import com.topicscanner.scanner.SourceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Scans Stack Overflow for topics using the public REST API v2.3.
 */
@Component
public class StackOverflowScanner implements SourceScanner {

    private static final Logger logger = LoggerFactory.getLogger(StackOverflowScanner.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String BASE_URL = "https://api.stackexchange.com/2.3";
    private static final long REQUEST_DELAY_MS = 2000;

    private final WebClient webClient;

    public StackOverflowScanner(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.clone()
                .baseUrl(BASE_URL)
                .build();
    }

    @Override
    public String getSourceType() {
        return "stackoverflow";
    }

    @Override
    public String getDisplayName() {
        return "Stack Overflow";
    }

    @Override
    public List<ScanResult> scan(ScanRequest request) {
        List<ScanResult> results = new ArrayList<>();
        String query = String.join(" ", request.keywords());

        if (query.isBlank()) {
            logger.warn("StackOverflow scan skipped: no keywords provided");
            return results;
        }

        int page = 1;
        int maxPages = 3;
        boolean hasMore = true;

        while (hasMore && page <= maxPages && results.size() < request.maxResults()) {
            try {
                List<ScanResult> batch = fetchPage(query, page, request);
                if (batch.isEmpty()) {
                    break;
                }
                results.addAll(batch);
                page++;
                if (page <= maxPages) {
                    Thread.sleep(REQUEST_DELAY_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.warn("StackOverflow scan failed on page {}: {}", page, e.getMessage());
                hasMore = false;
            }
        }

        return results.stream()
                .limit(request.maxResults())
                .toList();
    }

    private List<ScanResult> fetchPage(String query, int page, ScanRequest request) {
        String body = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("order", "desc")
                        .queryParam("sort", "relevance")
                        .queryParam("intitle", query)
                        .queryParam("site", "stackoverflow")
                        .queryParam("filter", "withbody")
                        .queryParam("pagesize", 25)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (body == null) {
            return List.of();
        }

        List<ScanResult> results = new ArrayList<>();
        try {
            JsonNode root = MAPPER.readTree(body);
            JsonNode items = root.path("items");

            for (JsonNode item : items) {
                String title = item.path("title").asText("");
                String link = item.path("link").asText("");
                long creationDate = item.path("creation_date").asLong(0);

                if (title.isBlank() || link.isBlank()) {
                    continue;
                }

                if (matchesNegativeKeywords(title, request.negativeKeywords())) {
                    continue;
                }

                LocalDateTime sourceDate = creationDate > 0
                        ? LocalDateTime.ofInstant(Instant.ofEpochSecond(creationDate), ZoneOffset.UTC)
                        : null;

                List<String> tags = new ArrayList<>();
                for (JsonNode tag : item.path("tags")) {
                    tags.add(tag.asText());
                }

                results.add(new ScanResult(
                        title,
                        link,
                        getSourceType(),
                        Map.of(
                                "externalId", String.valueOf(item.path("question_id").asLong()),
                                "score", item.path("score").asInt(0),
                                "answerCount", item.path("answer_count").asInt(0),
                                "viewCount", item.path("view_count").asInt(0),
                                "isAnswered", item.path("is_answered").asBoolean(false),
                                "tags", tags
                        ),
                        sourceDate
                ));
            }
        } catch (Exception e) {
            logger.warn("Failed to parse StackOverflow response: {}", e.getMessage());
        }
        return results;
    }

    private boolean matchesNegativeKeywords(String text, List<String> negativeKeywords) {
        String lower = text.toLowerCase();
        return negativeKeywords.stream()
                .anyMatch(kw -> lower.contains(kw.toLowerCase()));
    }
}
