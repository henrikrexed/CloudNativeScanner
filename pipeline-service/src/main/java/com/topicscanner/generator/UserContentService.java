package com.topicscanner.generator;

import com.topicscanner.extraction.WebContentExtractor;
import com.topicscanner.llm.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages user-uploaded content for writing style learning.
 * Content is stored in the user_content table with embeddings for RAG retrieval.
 */
@Service
public class UserContentService {

    private static final Logger logger = LoggerFactory.getLogger(UserContentService.class);

    private final JdbcTemplate jdbcTemplate;
    private final LLMService llmService;
    private final WebContentExtractor webContentExtractor;

    public UserContentService(JdbcTemplate jdbcTemplate,
                               LLMService llmService,
                               WebContentExtractor webContentExtractor) {
        this.jdbcTemplate = jdbcTemplate;
        this.llmService = llmService;
        this.webContentExtractor = webContentExtractor;
    }

    /**
     * Upload content directly (paste or file upload).
     */
    @Transactional
    public Long uploadContent(String title, String content, String contentType) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content cannot be empty");
        }

        String embedding = generateEmbedding(content);

        if (embedding != null) {
            List<Long> ids = jdbcTemplate.queryForList(
                    """
                    INSERT INTO user_content (title, content, content_type, embedding)
                    VALUES (?, ?, ?, ?::vector)
                    RETURNING id
                    """,
                    Long.class, title, content, contentType, embedding);
            Long id = ids.get(0);
            logger.info("Uploaded user content '{}' (id={}, type={})", title, id, contentType);
            return id;
        } else {
            List<Long> ids = jdbcTemplate.queryForList(
                    """
                    INSERT INTO user_content (title, content, content_type)
                    VALUES (?, ?, ?)
                    RETURNING id
                    """,
                    Long.class, title, content, contentType);
            Long id = ids.get(0);
            logger.info("Uploaded user content '{}' (id={}, type={}, no embedding)", title, id, contentType);
            return id;
        }
    }

    /**
     * Import content from a URL by extracting it first.
     */
    @Transactional
    public Long importFromUrl(String url) {
        Optional<String> extracted = webContentExtractor.extract(url);
        if (extracted.isEmpty()) {
            throw new IllegalArgumentException("Could not extract content from URL: " + url);
        }

        String title = extractTitleFromUrl(url);
        return uploadContent(title, extracted.get(), "blog_post");
    }

    /**
     * List all uploaded content.
     */
    public List<Map<String, Object>> listContent() {
        return jdbcTemplate.queryForList(
                """
                SELECT id, title, content_type,
                       writing_style_metadata IS NOT NULL AS style_analyzed,
                       LENGTH(content) AS content_length,
                       created_at
                FROM user_content
                ORDER BY created_at DESC
                """);
    }

    /**
     * Get a single content item by ID.
     */
    public Map<String, Object> getContent(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, title, content, content_type, writing_style_metadata, created_at FROM user_content WHERE id = ?",
                id);
        if (rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
    }

    /**
     * Delete content and its embedding.
     */
    @Transactional
    public boolean deleteContent(Long id) {
        int deleted = jdbcTemplate.update("DELETE FROM user_content WHERE id = ?", id);
        if (deleted > 0) {
            logger.info("Deleted user content id={}", id);
        }
        return deleted > 0;
    }

    /**
     * Find similar user content using pgvector cosine similarity for RAG retrieval.
     */
    public List<Map<String, Object>> findSimilarContent(String text, int limit) {
        String embedding = generateEmbedding(text);
        if (embedding == null) {
            return List.of();
        }

        return jdbcTemplate.queryForList(
                """
                SELECT id, title, content, content_type,
                       1 - (embedding <=> ?::vector) AS similarity
                FROM user_content
                WHERE embedding IS NOT NULL
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """,
                embedding, embedding, limit);
    }

    private String generateEmbedding(String text) {
        try {
            String truncated = text.length() > 2000 ? text.substring(0, 2000) : text;
            List<float[]> embeddings = llmService.embed(List.of(truncated));
            if (embeddings == null || embeddings.isEmpty()) {
                return null;
            }
            return toVectorString(embeddings.get(0));
        } catch (Exception e) {
            logger.warn("Failed to generate embedding: {}", e.getMessage());
            return null;
        }
    }

    private String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            float v = embedding[i];
            sb.append(Float.isNaN(v) || Float.isInfinite(v) ? "0.0" : v);
        }
        sb.append("]");
        return sb.toString();
    }

    private String extractTitleFromUrl(String url) {
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash > 0 && lastSlash < url.length() - 1) {
            String path = url.substring(lastSlash + 1);
            if (path.contains("?")) path = path.substring(0, path.indexOf("?"));
            return path.replace("-", " ").replace("_", " ");
        }
        return url;
    }
}
