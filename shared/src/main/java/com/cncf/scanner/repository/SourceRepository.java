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
     * Uses ScanHistory to determine last scan time - finds sources with no completed scan
     * or where the most recent completed scan is older than cutoffTime
     */
    @Query(value = "SELECT s FROM Source s WHERE s.isActive = true AND " +
           "(NOT EXISTS (SELECT 1 FROM ScanHistory sh WHERE sh.source = s AND sh.status = 'COMPLETED') " +
           "OR (SELECT MAX(sh.completedAt) FROM ScanHistory sh WHERE sh.source = s AND sh.status = 'COMPLETED') < :cutoffTime " +
           "OR (SELECT MAX(sh.completedAt) FROM ScanHistory sh WHERE sh.source = s AND sh.status = 'COMPLETED') IS NULL)",
           countQuery = "SELECT COUNT(s) FROM Source s WHERE s.isActive = true AND " +
           "(NOT EXISTS (SELECT 1 FROM ScanHistory sh WHERE sh.source = s AND sh.status = 'COMPLETED') " +
           "OR (SELECT MAX(sh.completedAt) FROM ScanHistory sh WHERE sh.source = s AND sh.status = 'COMPLETED') < :cutoffTime " +
           "OR (SELECT MAX(sh.completedAt) FROM ScanHistory sh WHERE sh.source = s AND sh.status = 'COMPLETED') IS NULL)")
    List<Source> findSourcesNeedingScan(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Find source by name
     */
    Optional<Source> findByName(String name);
}
