package com.topicscanner.generator;

import com.topicscanner.llm.LLMResponse;
import com.topicscanner.llm.LLMService;
import com.topicscanner.llm.LLMTaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Analyzes writing style from user-uploaded content.
 * Produces a style profile (tone, sentence patterns, vocabulary) stored
 * in user_content.writing_style_metadata as JSON.
 */
@Service
public class StyleAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(StyleAnalysisService.class);

    private static final String STYLE_SYSTEM_PROMPT = """
            You are a writing style analyst. Analyze the given text and produce a JSON style profile.
            Return ONLY valid JSON with no markdown formatting, no code fences.

            JSON schema:
            {
              "tone": "formal|casual|conversational|technical|mixed",
              "avg_sentence_length": "short|medium|long",
              "vocabulary_complexity": "simple|moderate|advanced",
              "structural_patterns": ["uses headers", "code examples", "bullet points", ...],
              "use_of_examples": true|false,
              "use_of_analogies": true|false,
              "first_person": true|false,
              "typical_intro_style": "description of how they start articles",
              "typical_conclusion_style": "description of how they end articles"
            }
            """;

    private final JdbcTemplate jdbcTemplate;
    private final LLMService llmService;

    public StyleAnalysisService(JdbcTemplate jdbcTemplate, LLMService llmService) {
        this.jdbcTemplate = jdbcTemplate;
        this.llmService = llmService;
    }

    /**
     * Analyze writing style for a single content item and store the result.
     */
    @Transactional
    public String analyzeContent(Long contentId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT content FROM user_content WHERE id = ?", contentId);

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Content not found: " + contentId);
        }

        String content = (String) rows.get(0).get("content");
        String truncated = content.length() > 4000 ? content.substring(0, 4000) : content;

        LLMResponse response = llmService.complete(LLMTaskType.SUMMARIZATION,
                STYLE_SYSTEM_PROMPT,
                "Analyze the writing style of this text:\n\n" + truncated);

        String styleMetadata = response.text();

        jdbcTemplate.update(
                "UPDATE user_content SET writing_style_metadata = ? WHERE id = ?",
                styleMetadata, contentId);

        logger.info("Style analysis completed for content id={}", contentId);
        return styleMetadata;
    }

    /**
     * Generate a composite style profile across all uploaded content.
     * Used as context for content generation.
     */
    public String getCompositeStyleProfile() {
        List<Map<String, Object>> styles = jdbcTemplate.queryForList(
                """
                SELECT writing_style_metadata
                FROM user_content
                WHERE writing_style_metadata IS NOT NULL
                ORDER BY created_at DESC
                LIMIT 10
                """);

        if (styles.isEmpty()) {
            return null;
        }

        if (styles.size() == 1) {
            return (String) styles.get(0).get("writing_style_metadata");
        }

        // Merge multiple style analyses into a composite profile
        StringBuilder allStyles = new StringBuilder();
        for (int i = 0; i < styles.size(); i++) {
            allStyles.append("Sample ").append(i + 1).append(": ");
            allStyles.append(styles.get(i).get("writing_style_metadata"));
            allStyles.append("\n\n");
        }

        LLMResponse response = llmService.complete(LLMTaskType.SUMMARIZATION,
                """
                You are a writing style analyst. Given multiple style analyses from the same author,
                produce a single composite style profile as JSON. Return ONLY valid JSON.
                Use the same schema as the input analyses. Identify the dominant patterns across all samples.
                """,
                "Merge these style analyses into one composite profile:\n\n" + allStyles);

        return response.text();
    }
}
