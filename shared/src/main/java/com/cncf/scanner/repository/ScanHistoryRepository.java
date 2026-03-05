package com.cncf.scanner.repository;

import com.cncf.scanner.model.ScanHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScanHistoryRepository extends JpaRepository<ScanHistory, Long> {
    
    /**
     * Find scan history by source
     */
    List<ScanHistory> findBySourceIdOrderByStartedAtDesc(Long sourceId);
    
    /**
     * Find the last successful scan for a source
     */
    @Query(value = "SELECT sh FROM ScanHistory sh WHERE sh.source.id = :sourceId AND sh.status = 'COMPLETED' ORDER BY sh.completedAt DESC",
           countQuery = "SELECT COUNT(sh) FROM ScanHistory sh WHERE sh.source.id = :sourceId AND sh.status = 'COMPLETED'")
    List<ScanHistory> findLastSuccessfulScan(@Param("sourceId") Long sourceId);
    
    /**
     * Find running scans
     */
    @Query(value = "SELECT sh FROM ScanHistory sh WHERE sh.status = 'RUNNING'",
           countQuery = "SELECT COUNT(sh) FROM ScanHistory sh WHERE sh.status = 'RUNNING'")
    List<ScanHistory> findRunningScans();
    
    /**
     * Find scans within a time range
     */
    @Query(value = "SELECT sh FROM ScanHistory sh WHERE sh.startedAt BETWEEN :startTime AND :endTime",
           countQuery = "SELECT COUNT(sh) FROM ScanHistory sh WHERE sh.startedAt BETWEEN :startTime AND :endTime")
    List<ScanHistory> findScansInTimeRange(@Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);
}
