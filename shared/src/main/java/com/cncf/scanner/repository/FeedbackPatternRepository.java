package com.cncf.scanner.repository;

import com.cncf.scanner.model.FeedbackPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackPatternRepository extends JpaRepository<FeedbackPattern, Long> {
    
    /**
     * Find all AVOID patterns (to filter out)
     */
    @Query("SELECT p FROM FeedbackPattern p WHERE p.patternType = 'AVOID' ORDER BY p.confidenceScore DESC, p.usageCount DESC")
    List<FeedbackPattern> findAvoidPatterns();
    
    /**
     * Find all PRIORITIZE patterns (to boost)
     */
    @Query("SELECT p FROM FeedbackPattern p WHERE p.patternType = 'PRIORITIZE' ORDER BY p.confidenceScore DESC, p.usageCount DESC")
    List<FeedbackPattern> findPrioritizePatterns();
    
    /**
     * Find patterns for a specific source
     */
    @Query("SELECT p FROM FeedbackPattern p WHERE p.source.id = :sourceId ORDER BY p.confidenceScore DESC")
    List<FeedbackPattern> findBySourceId(@Param("sourceId") Long sourceId);
    
    /**
     * Find patterns for a specific theme
     */
    @Query("SELECT p FROM FeedbackPattern p WHERE p.theme.id = :themeId ORDER BY p.confidenceScore DESC")
    List<FeedbackPattern> findByThemeId(@Param("themeId") Long themeId);
    
    /**
     * Find patterns by type and source
     */
    @Query("SELECT p FROM FeedbackPattern p WHERE p.patternType = :patternType AND p.source.id = :sourceId ORDER BY p.confidenceScore DESC")
    List<FeedbackPattern> findByTypeAndSource(@Param("patternType") String patternType, @Param("sourceId") Long sourceId);
    
    /**
     * Check if a pattern already exists
     */
    @Query("SELECT COUNT(p) > 0 FROM FeedbackPattern p WHERE p.patternText = :patternText AND p.patternType = :patternType")
    boolean existsByPatternTextAndType(@Param("patternText") String patternText, @Param("patternType") String patternType);
}

