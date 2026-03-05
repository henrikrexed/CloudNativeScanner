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

/**
 * Scans Hashnode for topics using the GraphQL API.
 */
@Component
public class HashnodeScanner implements SourceScanner {

    private static final Logger logger = LoggerFactory.getLogger(HashnodeScanner.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String GRAPHQL_URL = "https://gql.hashnode.com";

    private final WebClient webClient;

    public HashnodeScanner(WebClient.Builder webClientBuilder,
                           @Value("${HASHNODE_API_TOKEN:}") String apiToken) {
        WebClient.Builder builder = webClientBuilder.clone()
                .baseUrl(GRAPHQL_URL)
                .defaultHeader("Content-Type", "application/json");

        if (apiToken != null && !apiToken.isBlank()) {
            builder.defaultHeader("Authorization", apiToken);
        }

        this.webClient = builder.build();
    }

    @Override
    public String getSourceType() {
        return "hashnode";
    }

    @Override
    public String getDisplayName() {
        return "Hashnode";
    }

    @Override
    public List<ScanResult> scan(ScanRequest request) {
        List<ScanResult> results = new ArrayList<>();
        String query = String.join(" ", request.keywords());

        if (query.isBlank()) {
            logger.warn("Hashnode scan skipped: no keywords provided");
            return results;
        }

        try {
            String graphqlQuery = buildSearchQuery(query, Math.min(request.maxResults(), 20));
            String requestBody = MAPPER.writeValueAsString(Map.of("query", graphqlQuery));

            String body = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (body == null) {
                return results;
            }

            JsonNode root = MAPPER.readTree(body);
            JsonNode edges = root.path("data").path("searchPostsOfPublication").path("edges");

            // Hashnode API may return different structures, try alternate path
            if (edges.isMissingNode() || !edges.isArray()) {
                edges = root.path("data").path("feed").path("edges");
            }
            if (edges.isMissingNode() || !edges.isArray()) {
                // Try tag-based search response
                JsonNode posts = root.path("data").path("tagPosts").path("edges");
                if (posts.isArray()) {
                    edges = posts;
                }
            }

            if (!edges.isArray()) {
                logger.debug("No results from Hashnode for query: {}", query);
                return results;
            }

            for (JsonNode edge : edges) {
                JsonNode node = edge.path("node");
                String title = node.path("title").asText("");
                String url = node.path("url").asText("");

                if (url.isBlank()) {
                    // Build URL from slug and publication
                    String slug = node.path("slug").asText("");
                    String publication = node.path("publication").path("url").asText("");
                    if (!slug.isBlank() && !publication.isBlank()) {
                        url = publication + (publication.endsWith("/") ? "" : "/") + slug;
                    }
                }

                if (title.isBlank() || url.isBlank()) {
                    continue;
                }

                if (matchesNegativeKeywords(title, request.negativeKeywords())) {
                    continue;
                }

                LocalDateTime sourceDate = parseDate(node.path("publishedAt").asText(""));

                results.add(new ScanResult(
                        title,
                        url,
                        getSourceType(),
                        Map.of(
                                "externalId", node.path("id").asText(""),
                                "brief", node.path("brief").asText("").substring(0,
                                        Math.min(node.path("brief").asText("").length(), 200)),
                                "reactionCount", node.path("reactionCount").asInt(0),
                                "replyCount", node.path("replyCount").asInt(0)
                        ),
                        sourceDate
                ));
            }
        } catch (Exception e) {
            logger.warn("Hashnode scan failed: {}", e.getMessage());
        }

        return results.stream()
                .limit(request.maxResults())
                .toList();
    }

    private String buildSearchQuery(String searchTerm, int first) {
        return """
                {
                  feed(first: %d, filter: { type: RELEVANT }) {
                    edges {
                      node {
                        id
                        title
                        brief
                        slug
                        url
                        publishedAt
                        reactionCount
                        replyCount
                        publication {
                          url
                        }
                      }
                    }
                  }
                }
                """.formatted(first);
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
