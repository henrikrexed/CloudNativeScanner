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
import java.util.Optional;

/**
 * Scans Reddit for topics using the public JSON API.
 * Searches subreddits matching the provided keywords.
 */
@Component
public class RedditScanner implements SourceScanner {

    private static final Logger logger = LoggerFactory.getLogger(RedditScanner.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String BASE_URL = "https://www.reddit.com";
    private static final long REQUEST_DELAY_MS = 6000;

    private final WebClient webClient;

    public RedditScanner(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.clone()
                .baseUrl(BASE_URL)
                .defaultHeader("User-Agent", "TopicScanner/2.0 (DevRel Intelligence)")
                .build();
    }

    @Override
    public String getSourceType() {
        return "reddit";
    }

    @Override
    public String getDisplayName() {
        return "Reddit";
    }

    @Override
    public List<ScanResult> scan(ScanRequest request) {
        List<ScanResult> results = new ArrayList<>();
        String query = String.join(" ", request.keywords());

        if (query.isBlank()) {
            logger.warn("Reddit scan skipped: no keywords provided");
            return results;
        }

        List<String> subreddits = resolveSubreddits(request);

        for (String subreddit : subreddits) {
            if (results.size() >= request.maxResults()) {
                break;
            }
            try {
                List<ScanResult> batch = searchSubreddit(subreddit, query, request);
                results.addAll(batch);
                if (subreddits.size() > 1) {
                    Thread.sleep(REQUEST_DELAY_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.warn("Reddit scan failed for r/{}: {}", subreddit, e.getMessage());
            }
        }

        return results.stream()
                .limit(request.maxResults())
                .toList();
    }

    @Override
    public Optional<String> extractContent(String url) {
        try {
            String jsonUrl = url.endsWith("/") ? url + ".json" : url + "/.json";
            String body = webClient.get()
                    .uri(jsonUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (body == null) {
                return Optional.empty();
            }

            JsonNode root = MAPPER.readTree(body);
            StringBuilder content = new StringBuilder();

            // First element is the post
            JsonNode postData = root.get(0).path("data").path("children").get(0).path("data");
            String selftext = postData.path("selftext").asText("");
            if (!selftext.isBlank()) {
                content.append(selftext).append("\n\n");
            }

            // Second element is the comments
            JsonNode comments = root.get(1).path("data").path("children");
            for (JsonNode comment : comments) {
                String commentBody = comment.path("data").path("body").asText("");
                if (!commentBody.isBlank() && !comment.path("data").path("stickied").asBoolean()) {
                    content.append(commentBody).append("\n\n");
                }
            }

            return content.isEmpty() ? Optional.empty() : Optional.of(content.toString().trim());
        } catch (Exception e) {
            logger.warn("Reddit content extraction failed for {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    private List<ScanResult> searchSubreddit(String subreddit, String query, ScanRequest request) {
        String body = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/r/{subreddit}/search.json")
                        .queryParam("q", query)
                        .queryParam("restrict_sr", "on")
                        .queryParam("sort", "relevance")
                        .queryParam("t", "week")
                        .queryParam("limit", Math.min(request.maxResults(), 25))
                        .build(subreddit))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (body == null) {
            return List.of();
        }

        List<ScanResult> results = new ArrayList<>();
        try {
            JsonNode root = MAPPER.readTree(body);
            JsonNode children = root.path("data").path("children");

            for (JsonNode child : children) {
                JsonNode data = child.path("data");
                String title = data.path("title").asText("");
                String permalink = data.path("permalink").asText("");
                String url = BASE_URL + permalink;
                long createdUtc = data.path("created_utc").asLong(0);

                if (title.isBlank() || permalink.isBlank()) {
                    continue;
                }

                if (matchesNegativeKeywords(title, request.negativeKeywords())) {
                    continue;
                }

                LocalDateTime sourceDate = createdUtc > 0
                        ? LocalDateTime.ofInstant(Instant.ofEpochSecond(createdUtc), ZoneOffset.UTC)
                        : null;

                results.add(new ScanResult(
                        title,
                        url,
                        getSourceType(),
                        Map.of(
                                "externalId", data.path("id").asText(""),
                                "subreddit", subreddit,
                                "score", data.path("score").asInt(0),
                                "numComments", data.path("num_comments").asInt(0),
                                "upvoteRatio", data.path("upvote_ratio").asDouble(0)
                        ),
                        sourceDate
                ));
            }
        } catch (Exception e) {
            logger.warn("Failed to parse Reddit response for r/{}: {}", subreddit, e.getMessage());
        }
        return results;
    }

    private List<String> resolveSubreddits(ScanRequest request) {
        Object configured = request.scannerConfig().get("subreddits");
        if (configured instanceof List<?> list && !list.isEmpty()) {
            return list.stream().map(Object::toString).toList();
        }
        // Default tech subreddits
        return List.of("programming", "devops", "kubernetes", "cloudnative", "golang",
                "java", "python", "rust", "webdev", "machinelearning");
    }

    private boolean matchesNegativeKeywords(String text, List<String> negativeKeywords) {
        String lower = text.toLowerCase();
        return negativeKeywords.stream()
                .anyMatch(kw -> lower.contains(kw.toLowerCase()));
    }
}
