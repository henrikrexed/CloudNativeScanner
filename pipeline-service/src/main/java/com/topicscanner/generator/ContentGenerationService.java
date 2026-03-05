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
 * Generates content drafts from topic clusters, using the user's writing style
 * and similar uploaded content (RAG via pgvector) as context.
 */
@Service
public class ContentGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(ContentGenerationService.class);
    private static final int MAX_REGENERATIONS = 5;

    private final JdbcTemplate jdbcTemplate;
    private final LLMService llmService;
    private final StyleAnalysisService styleAnalysisService;
    private final UserContentService userContentService;

    public ContentGenerationService(JdbcTemplate jdbcTemplate,
                                     LLMService llmService,
                                     StyleAnalysisService styleAnalysisService,
                                     UserContentService userContentService) {
        this.jdbcTemplate = jdbcTemplate;
        this.llmService = llmService;
        this.styleAnalysisService = styleAnalysisService;
        this.userContentService = userContentService;
    }

    /**
     * Generate a content draft from a topic or topic cluster.
     *
     * @param topicId the primary topic ID (or null if using groupId)
     * @param groupId the topic cluster group ID (optional)
     * @param outputFormat blog_post, youtube_script, linkedin_post, newsletter
     * @return the generated content ID
     */
    @Transactional
    public Long generate(Long topicId, String groupId, String outputFormat) {
        // Load topic content
        StringBuilder topicContext = new StringBuilder();
        if (topicId != null) {
            List<Map<String, Object>> topics = jdbcTemplate.queryForList(
                    "SELECT title, extracted_content, summary, url, source_date FROM topics WHERE id = ?",
                    topicId);
            if (topics.isEmpty()) {
                throw new IllegalArgumentException("Topic not found: " + topicId);
            }
            appendTopicToContext(topicContext, topics.get(0));

            // If topic has a group, also include related topics
            if (groupId == null) {
                List<Map<String, Object>> topicRow = jdbcTemplate.queryForList(
                        "SELECT group_id FROM topics WHERE id = ?", topicId);
                if (!topicRow.isEmpty()) {
                    groupId = (String) topicRow.get(0).get("group_id");
                }
            }
        }

        // Load cluster topics if group_id is available
        if (groupId != null) {
            List<Map<String, Object>> clusterTopics = jdbcTemplate.queryForList(
                    """
                    SELECT title, extracted_content, summary, url, source_date
                    FROM topics
                    WHERE group_id = ? AND id != COALESCE(?, -1)
                    ORDER BY quality_score DESC NULLS LAST
                    LIMIT 5
                    """,
                    groupId, topicId);
            for (Map<String, Object> topic : clusterTopics) {
                topicContext.append("\n---\nRelated article:\n");
                appendTopicToContext(topicContext, topic);
            }
        }

        if (topicContext.isEmpty()) {
            throw new IllegalArgumentException("No topic content found for generation");
        }

        // Get user's style profile
        String styleProfile = styleAnalysisService.getCompositeStyleProfile();

        // RAG: find similar user content
        String ragContext = buildRagContext(topicContext.toString());

        // Build prompts
        String systemPrompt = buildSystemPrompt(outputFormat, styleProfile, ragContext);
        String userPrompt = buildUserPrompt(outputFormat, topicContext.toString());

        // Generate
        LLMResponse response = llmService.complete(LLMTaskType.GENERATION, systemPrompt, userPrompt);
        String generatedText = response.text();

        // Store
        List<Long> ids = jdbcTemplate.queryForList(
                """
                INSERT INTO generated_content (topic_id, group_id, output_format, generated_text, prompt_used, model_used)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                Long.class, topicId, groupId, outputFormat, generatedText,
                truncate(userPrompt, 5000), response.model());

        Long id = ids.get(0);
        logger.info("Generated {} content (id={}) for topic {}", outputFormat, id, topicId);
        return id;
    }

    /**
     * Regenerate content with user feedback.
     */
    @Transactional
    public Long regenerate(Long generatedContentId, String feedback) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT topic_id, group_id, output_format, generated_text, model_used
                FROM generated_content
                WHERE id = ?
                """,
                generatedContentId);

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Generated content not found: " + generatedContentId);
        }

        Map<String, Object> original = rows.get(0);

        // Check regeneration count
        Long topicId = (Long) original.get("topic_id");
        String groupId = (String) original.get("group_id");
        String outputFormat = (String) original.get("output_format");

        long regenerationCount = jdbcTemplate.queryForList(
                "SELECT COUNT(*) FROM generated_content WHERE topic_id = ? AND output_format = ?",
                Long.class, topicId, outputFormat).get(0);

        if (regenerationCount >= MAX_REGENERATIONS) {
            throw new IllegalStateException("Maximum regeneration rounds reached (" + MAX_REGENERATIONS + ")");
        }

        String originalText = (String) original.get("generated_text");

        // Regenerate with feedback
        String systemPrompt = """
                You are a content editor. The user has provided feedback on a draft.
                Revise the draft according to their feedback while maintaining the overall structure and topic coverage.
                Return the complete revised draft.
                """;

        String userPrompt = """
                Original draft:
                %s

                User feedback:
                %s

                Please revise the draft according to this feedback.
                """.formatted(originalText, feedback);

        LLMResponse response = llmService.complete(LLMTaskType.GENERATION, systemPrompt, userPrompt);

        // Store feedback on original
        jdbcTemplate.update(
                "UPDATE generated_content SET feedback = ? WHERE id = ?",
                feedback, generatedContentId);

        // Store new version
        List<Long> ids = jdbcTemplate.queryForList(
                """
                INSERT INTO generated_content (topic_id, group_id, output_format, generated_text, prompt_used, model_used)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                Long.class, topicId, groupId, outputFormat, response.text(),
                truncate(userPrompt, 5000), response.model());

        Long id = ids.get(0);
        logger.info("Regenerated {} content (id={}, feedback on id={})", outputFormat, id, generatedContentId);
        return id;
    }

    /**
     * Get generated content by ID.
     */
    public Map<String, Object> getGeneratedContent(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT gc.id, gc.topic_id, gc.group_id, gc.output_format,
                       gc.generated_text, gc.model_used, gc.feedback, gc.created_at,
                       t.title AS topic_title
                FROM generated_content gc
                LEFT JOIN topics t ON gc.topic_id = t.id
                WHERE gc.id = ?
                """,
                id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * List generated content, optionally filtered by topic.
     */
    public List<Map<String, Object>> listGeneratedContent(Long topicId) {
        if (topicId != null) {
            return jdbcTemplate.queryForList(
                    """
                    SELECT gc.id, gc.output_format, gc.model_used, gc.created_at,
                           LEFT(gc.generated_text, 200) AS preview,
                           t.title AS topic_title
                    FROM generated_content gc
                    LEFT JOIN topics t ON gc.topic_id = t.id
                    WHERE gc.topic_id = ?
                    ORDER BY gc.created_at DESC
                    """,
                    topicId);
        }
        return jdbcTemplate.queryForList(
                """
                SELECT gc.id, gc.output_format, gc.model_used, gc.created_at,
                       LEFT(gc.generated_text, 200) AS preview,
                       t.title AS topic_title
                FROM generated_content gc
                LEFT JOIN topics t ON gc.topic_id = t.id
                ORDER BY gc.created_at DESC
                LIMIT 50
                """);
    }

    /**
     * Export content as Markdown.
     */
    public String exportAsMarkdown(Long id) {
        Map<String, Object> content = getGeneratedContent(id);
        if (content == null) {
            return null;
        }
        return (String) content.get("generated_text");
    }

    private void appendTopicToContext(StringBuilder sb, Map<String, Object> topic) {
        sb.append("Title: ").append(topic.get("title")).append("\n");
        String summary = (String) topic.get("summary");
        if (summary != null && !summary.isBlank()) {
            sb.append("Summary: ").append(summary).append("\n");
        }
        String content = (String) topic.get("extracted_content");
        if (content != null) {
            sb.append("Content:\n").append(truncate(content, 3000)).append("\n");
        }
        sb.append("Source: ").append(topic.get("url")).append("\n");
    }

    private String buildRagContext(String topicText) {
        List<Map<String, Object>> similar = userContentService.findSimilarContent(topicText, 3);
        if (similar.isEmpty()) {
            return "";
        }

        StringBuilder rag = new StringBuilder();
        for (Map<String, Object> item : similar) {
            rag.append("--- Reference content (").append(item.get("title")).append(") ---\n");
            rag.append(truncate((String) item.get("content"), 1500)).append("\n\n");
        }
        return rag.toString();
    }

    private String buildSystemPrompt(String outputFormat, String styleProfile, String ragContext) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are a professional content writer. ");

        switch (outputFormat) {
            case "youtube_script" -> sb.append(
                    "Write a YouTube video script with an engaging hook, clear sections, and a strong call-to-action. " +
                    "Include [VISUAL] cues for on-screen elements. Target 8-12 minutes of speaking time.");
            case "linkedin_post" -> sb.append(
                    "Write a LinkedIn post (under 3000 characters). Start with a hook. Use short paragraphs. " +
                    "End with a question or call-to-action. Include relevant hashtags.");
            case "newsletter" -> sb.append(
                    "Write a newsletter section. Conversational tone, scannable format with headers and bullets. " +
                    "Include key takeaways and links to original sources.");
            default -> sb.append(
                    "Write a comprehensive blog post with a clear introduction, structured body with headings, " +
                    "code examples where relevant, and a conclusion with key takeaways.");
        }

        if (styleProfile != null && !styleProfile.isBlank()) {
            sb.append("\n\nMatch this writing style:\n").append(styleProfile);
        }

        if (!ragContext.isBlank()) {
            sb.append("\n\nReference the following examples of the author's previous writing for style and voice:\n");
            sb.append(ragContext);
        }

        return sb.toString();
    }

    private String buildUserPrompt(String outputFormat, String topicContext) {
        return """
                Based on the following topic research, create a %s:

                %s

                Requirements:
                - Original content, not a summary of the source
                - Add your own analysis and insights
                - Use specific examples from the source material
                - Include proper attribution to sources
                """.formatted(outputFormat.replace("_", " "), topicContext);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
