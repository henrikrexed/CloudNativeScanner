package com.topicscanner.extraction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts structured Q&A content from StackOverflow questions via API.
 * Returns formatted text with question body, accepted answer, and top answers.
 */
@Component
public class StackOverflowExtractor {

    private static final Logger logger = LoggerFactory.getLogger(StackOverflowExtractor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String API_BASE = "https://api.stackexchange.com/2.3";
    private static final Pattern QUESTION_ID_PATTERN = Pattern.compile("/questions/(\\d+)");
    private static final int MAX_ANSWERS = 5;

    private final WebClient webClient;

    public StackOverflowExtractor(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.clone()
                .baseUrl(API_BASE)
                .build();
    }

    /**
     * Extract structured content from a StackOverflow question URL.
     *
     * @param url the StackOverflow question URL
     * @return formatted Q&A content, or empty if extraction fails
     */
    public Optional<String> extract(String url) {
        String questionId = extractQuestionId(url);
        if (questionId == null) {
            logger.debug("Could not extract question ID from URL: {}", url);
            return Optional.empty();
        }

        try {
            StringBuilder content = new StringBuilder();

            // Fetch question
            String questionBody = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/questions/{id}")
                            .queryParam("site", "stackoverflow")
                            .queryParam("filter", "withbody")
                            .build(questionId))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (questionBody == null) {
                return Optional.empty();
            }

            JsonNode questionRoot = MAPPER.readTree(questionBody);
            JsonNode questionItems = questionRoot.path("items");
            if (!questionItems.isArray() || questionItems.isEmpty()) {
                return Optional.empty();
            }

            JsonNode question = questionItems.get(0);
            content.append("## Question\n\n");
            content.append("**").append(question.path("title").asText("")).append("**\n\n");
            content.append(stripHtml(question.path("body").asText(""))).append("\n\n");
            content.append("Score: ").append(question.path("score").asInt(0));
            content.append(" | Views: ").append(question.path("view_count").asInt(0)).append("\n\n");

            // Fetch answers
            String answersBody = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/questions/{id}/answers")
                            .queryParam("site", "stackoverflow")
                            .queryParam("filter", "withbody")
                            .queryParam("sort", "votes")
                            .queryParam("order", "desc")
                            .queryParam("pagesize", MAX_ANSWERS)
                            .build(questionId))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (answersBody != null) {
                JsonNode answersRoot = MAPPER.readTree(answersBody);
                JsonNode answers = answersRoot.path("items");

                int answerNum = 0;
                for (JsonNode answer : answers) {
                    answerNum++;
                    boolean isAccepted = answer.path("is_accepted").asBoolean(false);
                    int score = answer.path("score").asInt(0);

                    content.append("## ").append(isAccepted ? "Accepted Answer" : "Answer " + answerNum);
                    content.append(" (Score: ").append(score).append(")\n\n");
                    content.append(stripHtml(answer.path("body").asText(""))).append("\n\n");
                }
            }

            String result = content.toString().trim();
            return result.isEmpty() ? Optional.empty() : Optional.of(result);
        } catch (Exception e) {
            logger.warn("StackOverflow extraction failed for {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    String extractQuestionId(String url) {
        Matcher matcher = QUESTION_ID_PATTERN.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String stripHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return org.jsoup.Jsoup.parse(html).text();
    }
}
