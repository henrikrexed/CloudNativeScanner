package com.cncf.scanner.repository;

import com.cncf.scanner.model.Topic;
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
     * Find topics by source
     */
    List<Topic> findBySourceId(Long sourceId);
    
    /**
     * Find topics that haven't been scanned since the given time
     */
    @Query("SELECT t FROM Topic t WHERE t.source.id = :sourceId AND t.lastScannedAt < :since")
    List<Topic> findTopicsNotScannedSince(@Param("sourceId") Long sourceId, @Param("since") LocalDateTime since);
    
    /**
     * Find topics by theme
     */
    @Query("SELECT t FROM Topic t JOIN t.topicThemes tt WHERE tt.theme.id = :themeId")
    List<Topic> findByThemeId(@Param("themeId") Long themeId);
    
    /**
     * Find recent topics
     */
    @Query("SELECT t FROM Topic t ORDER BY t.createdAt DESC")
    List<Topic> findRecentTopics();
    
    /**
     * Count topics by source
     */
    long countBySourceId(Long sourceId);
    
    /**
     * Find topics with high interaction count
     */
    @Query("SELECT t FROM Topic t WHERE t.interactionCount > :minInteractions ORDER BY t.interactionCount DESC")
    List<Topic> findPopularTopics(@Param("minInteractions") Integer minInteractions);
}
