package com.topicscanner.filter.impl;

import com.topicscanner.filter.FilterResult;
import com.topicscanner.filter.TopicContext;
import com.topicscanner.filter.TopicFilter;
import com.topicscanner.llm.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Filter 7: Semantic deduplication using embeddings and pgvector.
 * Order: 70 (expensive — LLM embedding + vector similarity query).
 * <p>
 * Generates an embedding for the topic and checks if any existing topic
 * has a similar embedding above the dedup threshold.
 */
@Component
public class EmbeddingDedupFilter implements TopicFilter {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingDedupFilter.class);

    private final LLMService llmService;
    private final JdbcTemplate jdbcTemplate;
    private final double similarityThreshold;

    public EmbeddingDedupFilter(LLMService llmService, JdbcTemplate jdbcTemplate,
                                 @Value("${topicscanner.pipeline.dedup-similarity-threshold:0.95}") double similarityThreshold) {
        this.llmService = llmService;
        this.jdbcTemplate = jdbcTemplate;
        this.similarityThreshold = similarityThreshold;
    }

    @Override
    public String getName() {
        return "embedding-dedup";
    }

    @Override
    public int getOrder() {
        return 70;
    }

    @Override
    public FilterResult apply(TopicContext context) {
        try {
            // Generate embedding for this topic
            String textToEmbed = context.getTitle() + " " +
                    truncate(context.getExtractedContent(), 1000);

            List<float[]> embeddings = llmService.embed(List.of(textToEmbed));

            if (embeddings == null || embeddings.isEmpty()) {
                logger.debug("No embedding returned for topic {}", context.getTopicId());
                return FilterResult.pass(getName());
            }

            float[] embedding = embeddings.get(0);
            String vectorStr = toVectorString(embedding);

            // Store embedding in context for later persistence
            context.setAttribute("embeddingVector", vectorStr);

            // Check for similar existing topics using pgvector cosine similarity
            // 1 - cosine_distance = cosine_similarity
            Double maxSimilarity = jdbcTemplate.queryForObject(
                    """
                    SELECT MAX(1 - (embedding <=> ?::vector))
                    FROM topics
                    WHERE id != ?
                      AND embedding IS NOT NULL
                      AND pipeline_stage = 'analyzed'
                    """,
                    Double.class,
                    vectorStr, context.getTopicId());

            if (maxSimilarity != null && maxSimilarity >= similarityThreshold) {
                return FilterResult.fail(getName(),
                        String.format("Similar topic found (similarity: %.3f, threshold: %.3f)",
                                maxSimilarity, similarityThreshold));
            }

            return FilterResult.pass(getName(), maxSimilarity != null ? 1.0 - maxSimilarity : 1.0);
        } catch (Exception e) {
            logger.warn("Embedding dedup failed for topic {}: {}", context.getTopicId(), e.getMessage());
            // On failure, pass through (don't block on embedding issues)
            return FilterResult.pass(getName());
        }
    }

    private String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
