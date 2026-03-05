package com.topicscanner.filter.impl;

import com.topicscanner.filter.FilterResult;
import com.topicscanner.filter.TopicContext;
import com.topicscanner.filter.TopicFilter;
import com.topicscanner.llm.LLMService;
import com.topicscanner.llm.LLMTaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Filter 5: Uses LLM to score topic relevance to the category.
 * Order: 50 (expensive — LLM call).
 */
@Component
public class RelevanceFilter implements TopicFilter {

    private static final Logger logger = LoggerFactory.getLogger(RelevanceFilter.class);

    private final LLMService llmService;
    private final double relevanceThreshold;

    public RelevanceFilter(LLMService llmService,
                           @Value("${topicscanner.pipeline.relevance-threshold:0.7}") double relevanceThreshold) {
        this.llmService = llmService;
        this.relevanceThreshold = relevanceThreshold;
    }

    @Override
    public String getName() {
        return "relevance";
    }

    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public FilterResult apply(TopicContext context) {
        String systemPrompt = """
                You are a topic relevance scorer. Score how relevant the given content is
                to the specified category and keywords.
                Respond with ONLY a number between 0.0 and 1.0 (no other text).
                0.0 = completely irrelevant, 1.0 = perfectly relevant.
                """;

        String userPrompt = """
                Category: %s
                Keywords: %s

                Title: %s

                Content (first 2000 chars):
                %s
                """.formatted(
                context.getCategoryName(),
                String.join(", ", context.getCategoryKeywords()),
                context.getTitle(),
                truncate(context.getExtractedContent(), 2000)
        );

        try {
            var response = llmService.complete(LLMTaskType.RELEVANCE, systemPrompt, userPrompt);
            double score = parseScore(response.text());

            context.setAttribute("relevanceScore", score);

            if (score < relevanceThreshold) {
                return FilterResult.fail(getName(),
                        String.format("Relevance score %.2f below threshold %.2f", score, relevanceThreshold));
            }

            return FilterResult.pass(getName(), score);
        } catch (Exception e) {
            logger.warn("LLM relevance scoring failed for topic {}: {}", context.getTopicId(), e.getMessage());
            // On LLM failure, pass the topic through (don't block on LLM issues)
            return FilterResult.pass(getName(), 0.5);
        }
    }

    private double parseScore(String response) {
        if (response == null) return 0.5;
        try {
            String cleaned = response.trim().replaceAll("[^0-9.]", "");
            double score = Double.parseDouble(cleaned);
            return Math.max(0.0, Math.min(1.0, score));
        } catch (NumberFormatException e) {
            logger.debug("Could not parse relevance score from: {}", response);
            return 0.5;
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
