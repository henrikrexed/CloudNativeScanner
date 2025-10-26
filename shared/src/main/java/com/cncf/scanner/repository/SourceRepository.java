package com.cncf.scanner.repository;

import com.cncf.scanner.model.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SourceRepository extends JpaRepository<Source, Long> {
    
    /**
     * Find active sources
     */
    List<Source> findByIsActiveTrue();
    
    /**
     * Find sources that need scanning based on frequency
     */
    @Query("SELECT s FROM Source s WHERE s.isActive = true AND " +
           "(s.lastScanTime IS NULL OR s.lastScanTime < :cutoffTime)")
    List<Source> findSourcesNeedingScan(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Find source by name
     */
    Optional<Source> findByName(String name);
}
