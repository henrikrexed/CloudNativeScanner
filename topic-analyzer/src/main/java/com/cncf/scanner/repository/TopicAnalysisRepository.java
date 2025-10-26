package com.cncf.scanner.repository;

import com.cncf.scanner.model.TopicAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicAnalysisRepository extends JpaRepository<TopicAnalysis, Long> {
    
    /**
     * Find analysis by topic ID
     */
    Optional<TopicAnalysis> findByTopicId(Long topicId);
    
    /**
     * Find analyses by relevance score range
     */
    List<TopicAnalysis> findByRelevanceScoreBetween(double minScore, double maxScore);
    
    /**
     * Find analyses by complexity level
     */
    List<TopicAnalysis> findByComplexityLevel(String complexityLevel);
    
    /**
     * Find high-relevance analyses
     */
    @Query("SELECT ta FROM TopicAnalysis ta WHERE ta.relevanceScore >= :minScore ORDER BY ta.relevanceScore DESC")
    List<TopicAnalysis> findHighRelevanceAnalyses(@Param("minScore") double minScore);
    
    /**
     * Find analyses by AI confidence
     */
    @Query("SELECT ta FROM TopicAnalysis ta WHERE ta.aiConfidence >= :minConfidence ORDER BY ta.aiConfidence DESC")
    List<TopicAnalysis> findByAiConfidence(@Param("minConfidence") double minConfidence);
    
    /**
     * Delete analysis by topic ID
     */
    void deleteByTopicId(Long topicId);
    
    /**
     * Count analyses by complexity level
     */
    long countByComplexityLevel(String complexityLevel);
    
    /**
     * Find recent analyses
     */
    @Query("SELECT ta FROM TopicAnalysis ta ORDER BY ta.createdAt DESC")
    List<TopicAnalysis> findRecentAnalyses();
}


