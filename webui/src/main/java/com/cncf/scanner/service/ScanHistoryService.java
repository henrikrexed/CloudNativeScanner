package com.cncf.scanner.service;

import com.cncf.scanner.model.ScanHistory;
import com.cncf.scanner.model.Source;
import com.cncf.scanner.repository.ScanHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ScanHistoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScanHistoryService.class);
    
    private final ScanHistoryRepository scanHistoryRepository;
    
    @Autowired
    public ScanHistoryService(ScanHistoryRepository scanHistoryRepository) {
        this.scanHistoryRepository = scanHistoryRepository;
    }
    
    /**
     * Start a new scan
     */
    public ScanHistory startScan(Source source) {
        ScanHistory scanHistory = new ScanHistory(source, LocalDateTime.now());
        return scanHistoryRepository.save(scanHistory);
    }
    
    /**
     * Complete a scan
     */
    public void completeScan(ScanHistory scanHistory) {
        scanHistory.setCompletedAt(LocalDateTime.now());
        scanHistory.setStatus("COMPLETED");
        scanHistoryRepository.save(scanHistory);
        
        logger.info("Scan completed for source {}: {} topics found, {} processed", 
                scanHistory.getSource().getName(), 
                scanHistory.getTopicsFound(), 
                scanHistory.getTopicsProcessed());
    }
    
    /**
     * Mark a scan as failed
     */
    public void failScan(ScanHistory scanHistory, String errorMessage) {
        scanHistory.setCompletedAt(LocalDateTime.now());
        scanHistory.setStatus("FAILED");
        scanHistory.setErrorMessage(errorMessage);
        scanHistoryRepository.save(scanHistory);
        
        logger.error("Scan failed for source {}: {}", 
                scanHistory.getSource().getName(), errorMessage);
    }
    
    /**
     * Get the last scan time for a source
     */
    public LocalDateTime getLastScanTime(Source source) {
        List<ScanHistory> lastScans = scanHistoryRepository.findLastSuccessfulScan(source.getId());
        if (!lastScans.isEmpty()) {
            return lastScans.get(0).getCompletedAt();
        }
        return null;
    }
    
    /**
     * Find scan history by source
     */
    public List<ScanHistory> findBySourceId(Long sourceId) {
        return scanHistoryRepository.findBySourceIdOrderByStartedAtDesc(sourceId);
    }
    
    /**
     * Find all scan history
     */
    public List<ScanHistory> findAll() {
        return scanHistoryRepository.findAll();
    }
    
    /**
     * Find running scans
     */
    public List<ScanHistory> findRunningScans() {
        return scanHistoryRepository.findRunningScans();
    }
    
    /**
     * Find scans in time range
     */
    public List<ScanHistory> findScansInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return scanHistoryRepository.findScansInTimeRange(startTime, endTime);
    }
    
    /**
     * Get scan statistics
     */
    public ScanStatistics getScanStatistics() {
        List<ScanHistory> allScans = findAll();
        
        long totalScans = allScans.size();
        long successfulScans = allScans.stream()
                .filter(scan -> "COMPLETED".equals(scan.getStatus()))
                .count();
        long failedScans = allScans.stream()
                .filter(scan -> "FAILED".equals(scan.getStatus()))
                .count();
        long runningScans = allScans.stream()
                .filter(scan -> "RUNNING".equals(scan.getStatus()))
                .count();
        
        int totalTopicsFound = allScans.stream()
                .mapToInt(scan -> scan.getTopicsFound() != null ? scan.getTopicsFound() : 0)
                .sum();
        
        int totalTopicsProcessed = allScans.stream()
                .mapToInt(scan -> scan.getTopicsProcessed() != null ? scan.getTopicsProcessed() : 0)
                .sum();
        
        return new ScanStatistics(totalScans, successfulScans, failedScans, runningScans, 
                totalTopicsFound, totalTopicsProcessed);
    }
    
    public static class ScanStatistics {
        private final long totalScans;
        private final long successfulScans;
        private final long failedScans;
        private final long runningScans;
        private final int totalTopicsFound;
        private final int totalTopicsProcessed;
        
        public ScanStatistics(long totalScans, long successfulScans, long failedScans, 
                            long runningScans, int totalTopicsFound, int totalTopicsProcessed) {
            this.totalScans = totalScans;
            this.successfulScans = successfulScans;
            this.failedScans = failedScans;
            this.runningScans = runningScans;
            this.totalTopicsFound = totalTopicsFound;
            this.totalTopicsProcessed = totalTopicsProcessed;
        }
        
        // Getters
        public long getTotalScans() { return totalScans; }
        public long getSuccessfulScans() { return successfulScans; }
        public long getFailedScans() { return failedScans; }
        public long getRunningScans() { return runningScans; }
        public int getTotalTopicsFound() { return totalTopicsFound; }
        public int getTotalTopicsProcessed() { return totalTopicsProcessed; }
        
        public double getSuccessRate() {
            return totalScans > 0 ? (double) successfulScans / totalScans * 100 : 0;
        }
    }
}


