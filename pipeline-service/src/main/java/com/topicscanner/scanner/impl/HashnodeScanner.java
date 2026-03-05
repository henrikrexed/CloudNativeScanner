package com.topicscanner.scanner.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.topicscanner.scanner.ScanRequest;
import com.topicscanner.scanner.ScanResult;
import com.topicscanner.scanner.ScannerUtils;
import com.topicscanner.scanner.SourceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
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
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final WebClient webClient;

    public HashnodeScanner(WebClient.Builder webClientBuilder,
                           @Value("${HASHNODE_API_TOKEN:}") String apiToken) {
        WebClient.Builder builder = webClientBuilder.clone()
                .baseUrl(GRAPHQL_URL)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("User-Agent", "TopicScanner/2.0 (DevRel Intelligence)");

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
                    .block(REQUEST_TIMEOUT);

            if (body == null) {
                return results;
            }

            JsonNode root = MAPPER.readTree(body);

            // Try multiple response paths as Hashnode API structure varies
            JsonNode edges = root.path("data").path("searchPostsOfPublication").path("edges");
            if (!edges.isArray() || edges.isEmpty()) {
                edges = root.path("data").path("feed").path("edges");
            }
            if (!edges.isArray() || edges.isEmpty()) {
                edges = root.path("data").path("tagPosts").path("edges");
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
                    String slug = node.path("slug").asText("");
                    String publication = node.path("publication").path("url").asText("");
                    if (!slug.isBlank() && !publication.isBlank()) {
                        url = publication + (publication.endsWith("/") ? "" : "/") + slug;
                    }
                }

                if (title.isBlank() || url.isBlank()) continue;
                if (ScannerUtils.matchesNegativeKeywords(title, request.negativeKeywords())) continue;

                var sourceDate = ScannerUtils.parseIsoDate(node.path("publishedAt").asText(""));

                String brief = node.path("brief").asText("");
                if (brief.length() > 200) {
                    brief = brief.substring(0, 200);
                }

                results.add(new ScanResult(title, url, getSourceType(),
                        Map.of(
                                "externalId", node.path("id").asText(""),
                                "brief", brief,
                                "reactionCount", node.path("reactionCount").asInt(0),
                                "replyCount", node.path("replyCount").asInt(0)
                        ),
                        sourceDate));
            }
        } catch (Exception e) {
            logger.warn("Hashnode scan failed: {}", e.getMessage());
        }

        return results.stream().limit(request.maxResults()).toList();
    }

    private String buildSearchQuery(String searchTerm, int first) {
        // Escape double quotes in search term for GraphQL
        String escapedTerm = searchTerm.replace("\"", "\\\"");
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
}
