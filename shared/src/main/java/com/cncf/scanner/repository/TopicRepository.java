package com.cncf.scanner.repository;

import com.cncf.scanner.model.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
    
    /**
     * Find topic by source and external ID
     */
    Optional<Topic> findBySourceIdAndExternalId(Long sourceId, String externalId);
    
    /**
     * Find topics by source (excluding rejected topics)
     */
    @Query(value = "SELECT t FROM Topic t WHERE t.source.id = :sourceId AND (t.isRejected IS NULL OR t.isRejected = false)",
           countQuery = "SELECT COUNT(t) FROM Topic t WHERE t.source.id = :sourceId AND (t.isRejected IS NULL OR t.isRejected = false)")
    List<Topic> findBySourceId(@Param("sourceId") Long sourceId);
    
    /**
     * Count topics by source (excluding rejected topics)
     */
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.source.id = :sourceId AND (t.isRejected IS NULL OR t.isRejected = false)")
    long countBySourceId(@Param("sourceId") Long sourceId);
    
    /**
     * Find topics that haven't been scanned since the given time
     */
    @Query(value = "SELECT t FROM Topic t WHERE t.source.id = :sourceId AND t.lastScannedAt < :since",
           countQuery = "SELECT COUNT(t) FROM Topic t WHERE t.source.id = :sourceId AND t.lastScannedAt < :since")
    List<Topic> findTopicsNotScannedSince(@Param("sourceId") Long sourceId, @Param("since") LocalDateTime since);
    
    /**
     * Find topics by theme (excluding rejected topics)
     * Returns topics directly linked to the theme OR linked to any of its sub-themes
     * Eagerly fetches source and topicThemes to avoid LazyInitializationException
     * 
     * This query finds:
     * 1. Topics directly linked to the theme (tt.theme.id = :themeId)
     * 2. Topics linked to sub-themes of the theme (sub-themes where parentTheme.id = :themeId)
     * 
     * Uses OR condition with a join to check if the theme is a sub-theme of the requested theme.
     * Note: Using JOIN FETCH for topicThemes to eagerly load all theme relationships.
     */
    @Query(value = "SELECT DISTINCT t FROM Topic t " +
           "LEFT JOIN FETCH t.source " +
           "JOIN FETCH t.topicThemes tt " +
           "LEFT JOIN FETCH tt.theme theme " +
           "LEFT JOIN FETCH theme.parentTheme " +
           "WHERE (tt.theme.id = :themeId OR tt.theme.parentTheme.id = :themeId) " +
           "AND (t.isRejected IS NULL OR t.isRejected = false) " +
           "ORDER BY t.engagementScore DESC NULLS LAST, t.interactionCount DESC, t.viewCount DESC, t.createdAt DESC",
           countQuery = "SELECT COUNT(DISTINCT t) FROM Topic t JOIN t.topicThemes tt " +
           "WHERE (tt.theme.id = :themeId OR tt.theme.parentTheme.id = :themeId) " +
           "AND (t.isRejected IS NULL OR t.isRejected = false)")
    List<Topic> findByThemeId(@Param("themeId") Long themeId);
    
    /**
     * Find recent topics (excluding rejected topics)
     */
    @Query(value = "SELECT t FROM Topic t WHERE (t.isRejected IS NULL OR t.isRejected = false) ORDER BY t.createdAt DESC",
           countQuery = "SELECT COUNT(t) FROM Topic t WHERE (t.isRejected IS NULL OR t.isRejected = false)")
    List<Topic> findRecentTopics();
    
    /**
     * Find topics with high interaction count
     */
    @Query(value = "SELECT t FROM Topic t WHERE t.interactionCount > :minInteractions ORDER BY t.interactionCount DESC",
           countQuery = "SELECT COUNT(t) FROM Topic t WHERE t.interactionCount > :minInteractions")
    List<Topic> findPopularTopics(@Param("minInteractions") Integer minInteractions);
    
    /**
     * Count topics without any theme classifications
     */
    @Query("SELECT COUNT(t) FROM Topic t WHERE NOT EXISTS (SELECT 1 FROM TopicTheme tt WHERE tt.topic.id = t.id)")
    long countTopicsWithoutThemes();
    
    /**
     * Check if a topic has any theme classifications
     */
    @Query("SELECT COUNT(tt) > 0 FROM TopicTheme tt WHERE tt.topic.id = :topicId")
    boolean hasTopicThemes(@Param("topicId") Long topicId);
    
    /**
     * Count topics created after a specific date
     */
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.createdAt >= :since")
    long countTopicsCreatedAfter(@Param("since") LocalDateTime since);
    
    /**
     * Find hottest topics sorted by engagement score (interactions, replies, views)
     * Excluding rejected topics
     */
    @Query(value = "SELECT t FROM Topic t WHERE (t.isRejected IS NULL OR t.isRejected = false) ORDER BY t.engagementScore DESC NULLS LAST, t.interactionCount DESC, t.viewCount DESC, t.createdAt DESC",
           countQuery = "SELECT COUNT(t) FROM Topic t WHERE (t.isRejected IS NULL OR t.isRejected = false)")
    List<Topic> findHottestTopics();
    
    /**
     * Find related topics by title similarity (case-insensitive partial match)
     * This finds topics with similar titles from different sources
     * Eagerly fetches source to avoid LazyInitializationException
     */
    @Query(value = "SELECT DISTINCT t FROM Topic t " +
           "LEFT JOIN FETCH t.source " +
           "WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :titleKeyword, '%')) AND t.id != :excludeId AND (t.isRejected IS NULL OR t.isRejected = false) " +
           "ORDER BY t.engagementScore DESC NULLS LAST, t.interactionCount DESC",
           countQuery = "SELECT COUNT(DISTINCT t) FROM Topic t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :titleKeyword, '%')) AND t.id != :excludeId AND (t.isRejected IS NULL OR t.isRejected = false)")
    List<Topic> findRelatedTopicsByTitle(@Param("titleKeyword") String titleKeyword, @Param("excludeId") Long excludeId);
    
    /**
     * Find topics by sub-theme (child theme)
     * Returns topics classified with the specified sub-theme or any of its parent themes
     */
    @Query(value = "SELECT DISTINCT t FROM Topic t JOIN t.topicThemes tt WHERE " +
           "(tt.theme.id = :subThemeId OR tt.theme.parentTheme.id = :subThemeId OR " +
           "(tt.theme.parentTheme IS NOT NULL AND tt.theme.parentTheme.id IN " +
           "(SELECT p.id FROM Theme p WHERE p.parentTheme.id = :subThemeId))) AND (t.isRejected IS NULL OR t.isRejected = false) " +
           "ORDER BY t.engagementScore DESC NULLS LAST, t.interactionCount DESC, t.viewCount DESC",
           countQuery = "SELECT COUNT(DISTINCT t) FROM Topic t JOIN t.topicThemes tt WHERE " +
           "(tt.theme.id = :subThemeId OR tt.theme.parentTheme.id = :subThemeId OR " +
           "(tt.theme.parentTheme IS NOT NULL AND tt.theme.parentTheme.id IN " +
           "(SELECT p.id FROM Theme p WHERE p.parentTheme.id = :subThemeId))) AND (t.isRejected IS NULL OR t.isRejected = false)")
    List<Topic> findBySubThemeId(@Param("subThemeId") Long subThemeId);
    
    /**
     * Find hottest topics by sub-theme (sorted by interactions/engagement)
     */
    @Query(value = "SELECT DISTINCT t FROM Topic t JOIN t.topicThemes tt WHERE " +
           "(tt.theme.id = :subThemeId OR tt.theme.parentTheme.id = :subThemeId) AND (t.isRejected IS NULL OR t.isRejected = false) " +
           "ORDER BY t.interactionCount DESC, t.engagementScore DESC NULLS LAST, t.viewCount DESC, t.createdAt DESC",
           countQuery = "SELECT COUNT(DISTINCT t) FROM Topic t JOIN t.topicThemes tt WHERE " +
           "(tt.theme.id = :subThemeId OR tt.theme.parentTheme.id = :subThemeId) AND (t.isRejected IS NULL OR t.isRejected = false)")
    List<Topic> findHottestTopicsBySubTheme(@Param("subThemeId") Long subThemeId);
    
    /**
     * Find hot topics that need conversation collection
     * Topics flagged as hot but without conversation summary (not yet processed)
     * OR topics with summary but not yet stored in RAG
     */
    @Query(value = "SELECT t FROM Topic t WHERE t.isHotTopic = true AND " +
           "((t.conversationSummary IS NULL OR t.conversationSummary = '' OR t.conversationCollectedAt IS NULL) OR " +
           "(t.conversationRagStored = false OR t.conversationRagStored IS NULL)) " +
           "ORDER BY t.engagementScore DESC NULLS LAST, t.interactionCount DESC",
           countQuery = "SELECT COUNT(t) FROM Topic t WHERE t.isHotTopic = true AND " +
           "((t.conversationSummary IS NULL OR t.conversationSummary = '' OR t.conversationCollectedAt IS NULL) OR " +
           "(t.conversationRagStored = false OR t.conversationRagStored IS NULL))")
    List<Topic> findHotTopicsNeedingProcessing();
    
    /**
     * Find all hot topics
     */
    @Query(value = "SELECT t FROM Topic t WHERE t.isHotTopic = true " +
           "ORDER BY t.engagementScore DESC NULLS LAST, t.interactionCount DESC",
           countQuery = "SELECT COUNT(t) FROM Topic t WHERE t.isHotTopic = true")
    List<Topic> findAllHotTopics();
    
    /**
     * Find topic by ID with eagerly fetched relationships (source and topicThemes)
     * This prevents LazyInitializationException when serializing the topic
     */
    @Query("SELECT DISTINCT t FROM Topic t " +
           "LEFT JOIN FETCH t.source " +
           "LEFT JOIN FETCH t.topicThemes tt " +
           "LEFT JOIN FETCH tt.theme theme " +
           "LEFT JOIN FETCH theme.parentTheme " +
           "WHERE t.id = :id")
    Optional<Topic> findByIdWithRelationships(@Param("id") Long id);
    
    /**
     * Find topics that need content extraction
     * Topics with PENDING status OR FAILED status (for retry)
     * For FAILED topics, retry threshold is configurable (default: immediate retry)
     * Not rejected, and have a URL
     * Eagerly fetches Source to avoid LazyInitializationException
     */
    @Query(value = "SELECT DISTINCT t FROM Topic t " +
           "LEFT JOIN FETCH t.source " +
           "WHERE ((t.contentExtractionStatus = 'PENDING') OR " +
           "       (t.contentExtractionStatus = 'FAILED' AND " +
           "        (t.contentExtractionAttemptedAt IS NULL OR t.contentExtractionAttemptedAt < :retryThreshold))) " +
           "AND (t.isRejected IS NULL OR t.isRejected = false) " +
           "AND t.url IS NOT NULL AND t.url != '' " +
           "ORDER BY t.createdAt ASC",
           countQuery = "SELECT COUNT(DISTINCT t) FROM Topic t WHERE " +
           "((t.contentExtractionStatus = 'PENDING') OR " +
           " (t.contentExtractionStatus = 'FAILED' AND " +
           "  (t.contentExtractionAttemptedAt IS NULL OR t.contentExtractionAttemptedAt < :retryThreshold))) " +
           "AND (t.isRejected IS NULL OR t.isRejected = false) " +
           "AND t.url IS NOT NULL AND t.url != ''")
    List<Topic> findTopicsNeedingContentExtraction(@Param("retryThreshold") LocalDateTime retryThreshold);
    
    /**
     * Find topics with content extraction status
     */
    @Query("SELECT t FROM Topic t WHERE t.contentExtractionStatus = :status " +
           "AND (t.isRejected IS NULL OR t.isRejected = false)")
    List<Topic> findByContentExtractionStatus(@Param("status") String status);
    
    /**
     * Count topics by content extraction status
     */
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.contentExtractionStatus = :status " +
           "AND (t.isRejected IS NULL OR t.isRejected = false)")
    long countByContentExtractionStatus(@Param("status") String status);
    
    /**
     * Count topics by source and content extraction status
     */
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.source.id = :sourceId " +
           "AND t.contentExtractionStatus = :status " +
           "AND (t.isRejected IS NULL OR t.isRejected = false)")
    long countBySourceIdAndContentExtractionStatus(@Param("sourceId") Long sourceId, @Param("status") String status);
    
    /**
     * Count topics with content summary (stored in RAG)
     */
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.contentSummary IS NOT NULL " +
           "AND t.contentSummary != '' " +
           "AND (t.isRejected IS NULL OR t.isRejected = false)")
    long countTopicsWithSummary();
    
    /**
     * Count topics in RAG (topics that are clustered, i.e., have topicGroupId)
     */
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.topicGroupId IS NOT NULL " +
           "AND (t.isRejected IS NULL OR t.isRejected = false)")
    long countTopicsInRAG();
    
    /**
     * Find topics without tags (or with empty tags)
     * Eagerly fetches source and topicThemes to avoid LazyInitializationException
     * Use Pageable to limit results (e.g., PageRequest.of(0, batchSize))
     */
    @Query(value = "SELECT DISTINCT t FROM Topic t " +
           "LEFT JOIN FETCH t.source " +
           "LEFT JOIN FETCH t.topicThemes tt " +
           "LEFT JOIN FETCH tt.theme theme " +
           "LEFT JOIN FETCH theme.parentTheme " +
           "WHERE (t.tags IS NULL OR t.tags = '' OR TRIM(t.tags) = '') " +
           "AND (t.isRejected IS NULL OR t.isRejected = false) " +
           "ORDER BY t.createdAt DESC",
           countQuery = "SELECT COUNT(DISTINCT t) FROM Topic t " +
           "WHERE (t.tags IS NULL OR t.tags = '' OR TRIM(t.tags) = '') " +
           "AND (t.isRejected IS NULL OR t.isRejected = false)")
    List<Topic> findTopicsWithoutTags(Pageable pageable);
    
    /**
     * Find topics by cluster/group ID (topicGroupId)
     * Eagerly fetches source to avoid LazyInitializationException
     * Excludes rejected topics
     */
    @Query(value = "SELECT DISTINCT t FROM Topic t " +
           "LEFT JOIN FETCH t.source " +
           "WHERE t.topicGroupId = :groupId " +
           "AND (t.isRejected IS NULL OR t.isRejected = false) " +
           "ORDER BY t.engagementScore DESC NULLS LAST, t.interactionCount DESC, t.viewCount DESC",
           countQuery = "SELECT COUNT(DISTINCT t) FROM Topic t " +
           "WHERE t.topicGroupId = :groupId " +
           "AND (t.isRejected IS NULL OR t.isRejected = false)")
    List<Topic> findByTopicGroupId(@Param("groupId") Long groupId);
    
    /**
     * Find all distinct cluster/group IDs (topicGroupId values)
     * Only returns groups that have at least one non-rejected topic
     */
    @Query("SELECT DISTINCT t.topicGroupId FROM Topic t " +
           "WHERE t.topicGroupId IS NOT NULL " +
           "AND (t.isRejected IS NULL OR t.isRejected = false) " +
           "ORDER BY t.topicGroupId")
    List<Long> findAllClusterIds();
    
    /**
     * Find cluster IDs filtered by theme
     * Returns cluster IDs that contain at least one topic linked to the specified theme or its sub-themes
     */
    @Query("SELECT DISTINCT t.topicGroupId FROM Topic t " +
           "JOIN t.topicThemes tt " +
           "WHERE t.topicGroupId IS NOT NULL " +
           "AND (tt.theme.id = :themeId OR tt.theme.parentTheme.id = :themeId) " +
           "AND (t.isRejected IS NULL OR t.isRejected = false) " +
           "ORDER BY t.topicGroupId")
    List<Long> findClusterIdsByThemeId(@Param("themeId") Long themeId);
    
    /**
     * Count topics in a cluster/group
     */
    @Query("SELECT COUNT(t) FROM Topic t " +
           "WHERE t.topicGroupId = :groupId " +
           "AND (t.isRejected IS NULL OR t.isRejected = false)")
    long countByTopicGroupId(@Param("groupId") Long groupId);
    
    /**
     * Find representative topic for a cluster (highest engagement score)
     * Returns the topic with the highest engagement score in the cluster
     */
    @Query(value = "SELECT t FROM Topic t " +
           "LEFT JOIN FETCH t.source " +
           "WHERE t.topicGroupId = :groupId " +
           "AND (t.isRejected IS NULL OR t.isRejected = false) " +
           "ORDER BY t.engagementScore DESC NULLS LAST, t.interactionCount DESC, t.viewCount DESC",
           countQuery = "SELECT COUNT(t) FROM Topic t " +
           "WHERE t.topicGroupId = :groupId " +
           "AND (t.isRejected IS NULL OR t.isRejected = false)")
    List<Topic> findRepresentativeTopicByGroupId(@Param("groupId") Long groupId, Pageable pageable);
    
    /**
     * Find a limited number of topics in a cluster for tag generation (optimized - limits at DB level)
     */
    @Query(value = "SELECT t FROM Topic t " +
           "LEFT JOIN FETCH t.source " +
           "WHERE t.topicGroupId = :groupId " +
           "AND (t.isRejected IS NULL OR t.isRejected = false) " +
           "ORDER BY t.engagementScore DESC NULLS LAST, t.interactionCount DESC, t.viewCount DESC")
    List<Topic> findSampleTopicsByGroupId(@Param("groupId") Long groupId, Pageable pageable);
    
    /**
     * Find topics by theme with database-level pagination
     */
    @Query(value = "SELECT DISTINCT t FROM Topic t " +
           "LEFT JOIN FETCH t.source " +
           "JOIN FETCH t.topicThemes tt " +
           "LEFT JOIN FETCH tt.theme theme " +
           "LEFT JOIN FETCH theme.parentTheme " +
           "WHERE (tt.theme.id = :themeId OR tt.theme.parentTheme.id = :themeId) " +
           "AND (t.isRejected IS NULL OR t.isRejected = false) " +
           "ORDER BY t.engagementScore DESC NULLS LAST, t.interactionCount DESC, t.viewCount DESC, t.createdAt DESC",
           countQuery = "SELECT COUNT(DISTINCT t) FROM Topic t JOIN t.topicThemes tt " +
           "WHERE (tt.theme.id = :themeId OR tt.theme.parentTheme.id = :themeId) " +
           "AND (t.isRejected IS NULL OR t.isRejected = false)")
    org.springframework.data.domain.Page<Topic> findByThemeIdPaginated(@Param("themeId") Long themeId, Pageable pageable);
}
