package com.cncf.scanner.repository;

import com.cncf.scanner.model.SearchTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SearchTopicRepository extends JpaRepository<SearchTopic, Long> {
    
    /**
     * Find search topics by source
     */
    List<SearchTopic> findBySourceId(Long sourceId);
    
    /**
     * Find active search topics by source
     */
    List<SearchTopic> findBySourceIdAndIsActiveTrue(Long sourceId);
    
    /**
     * Find search topics that need searching based on frequency
     */
    @Query(value = "SELECT st FROM SearchTopic st WHERE st.source.id = :sourceId AND st.isActive = true AND " +
           "(st.lastSearchedAt IS NULL OR st.lastSearchedAt < :cutoffTime)",
           countQuery = "SELECT COUNT(st) FROM SearchTopic st WHERE st.source.id = :sourceId AND st.isActive = true AND " +
           "(st.lastSearchedAt IS NULL OR st.lastSearchedAt < :cutoffTime)")
    List<SearchTopic> findSearchTopicsNeedingSearch(@Param("sourceId") Long sourceId, @Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Find search topics by priority
     */
    List<SearchTopic> findBySourceIdAndPriorityOrderByPriorityAsc(Long sourceId, Integer priority);
    
    /**
     * Find all active search topics ordered by priority
     */
    @Query(value = "SELECT st FROM SearchTopic st WHERE st.isActive = true ORDER BY st.priority ASC, st.keyword ASC",
           countQuery = "SELECT COUNT(st) FROM SearchTopic st WHERE st.isActive = true")
    List<SearchTopic> findAllActiveOrderByPriority();
    
    /**
     * Count search topics by source
     */
    long countBySourceId(Long sourceId);
    
    /**
     * Count active search topics by source
     */
    long countBySourceIdAndIsActiveTrue(Long sourceId);
}



