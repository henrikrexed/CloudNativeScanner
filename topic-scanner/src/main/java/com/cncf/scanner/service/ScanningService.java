package com.cncf.scanner.service;

import com.cncf.scanner.config.DebugConfig;
import com.cncf.scanner.kafka.TopicProducer;
import com.cncf.scanner.model.*;
import com.cncf.scanner.scanner.ScanResult;
import com.cncf.scanner.scanner.ScannerManager;
import com.cncf.scanner.util.ScanTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ScanningService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScanningService.class);
    
    private final SourceService sourceService;
    private final ScannerManager scannerManager;
    private final TopicProducer topicProducer;
    private final ScanHistoryService scanHistoryService;
    private final DebugConfig debugConfig;
    
    @Autowired
    public ScanningService(SourceService sourceService, 
                          ScannerManager scannerManager,
                          TopicProducer topicProducer,
                          ScanHistoryService scanHistoryService,
                          DebugConfig debugConfig) {
        this.sourceService = sourceService;
        this.scannerManager = scannerManager;
        this.topicProducer = topicProducer;
        this.scanHistoryService = scanHistoryService;
        this.debugConfig = debugConfig;
    }
    
    /**
     * Scheduled method to run daily scanning
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    public void performDailyScan() {
        ScanTracer.startTrace("DAILY_SCAN", "ALL_SOURCES", "Starting scheduled daily scan");
        Instant scanStart = Instant.now();
        
        logger.info("🚀 Starting daily scan of all active sources");
        
        try {
            List<Source> activeSources = sourceService.findActiveSources();
            logger.info("📋 Found {} active sources to scan", activeSources.size());
            
            Map<String, Object> scanMetrics = new HashMap<>();
            scanMetrics.put("totalSources", activeSources.size());
            scanMetrics.put("successfulScans", 0);
            scanMetrics.put("failedScans", 0);
            scanMetrics.put("totalTopicsFound", 0);
            
            for (Source source : activeSources) {
                try {
                    ScanTracer.logStep("DAILY_SCAN", "ALL_SOURCES", "SCANNING_SOURCE", 
                            "Starting scan for source: " + source.getName());
                    
                    scanSource(source);
                    scanMetrics.put("successfulScans", (Integer) scanMetrics.get("successfulScans") + 1);
                    
                } catch (Exception e) {
                    scanMetrics.put("failedScans", (Integer) scanMetrics.get("failedScans") + 1);
                    ScanTracer.logError("DAILY_SCAN", "ALL_SOURCES", 
                            "Failed to scan source: " + source.getName(), e);
                    logger.error("❌ Error scanning source {}: {}", source.getName(), e.getMessage(), e);
                }
            }
            
            Duration totalDuration = Duration.between(scanStart, Instant.now());
            scanMetrics.put("totalDurationMs", totalDuration.toMillis());
            
            ScanTracer.logMetrics("DAILY_SCAN", "ALL_SOURCES", scanMetrics);
            logger.info("✅ Completed daily scan of all active sources in {}ms - Success: {}, Failed: {}", 
                    totalDuration.toMillis(), scanMetrics.get("successfulScans"), scanMetrics.get("failedScans"));
            
        } catch (Exception e) {
            ScanTracer.logError("DAILY_SCAN", "ALL_SOURCES", "Daily scan failed", e);
            logger.error("💥 Daily scan failed: {}", e.getMessage(), e);
        } finally {
            ScanTracer.endTrace("DAILY_SCAN", "ALL_SOURCES", "Daily scan completed");
        }
    }
    
    /**
     * Scan a specific source
     */
    public void scanSource(Source source) {
        ScanTracer.startTrace("SOURCE_SCAN", source.getName(), 
                "Starting scan for source: " + source.getName() + " (ID: " + source.getId() + ")");
        Instant scanStart = Instant.now();
        
        logger.info("🔍 Starting scan for source: {} (ID: {})", source.getName(), source.getId());
        
        // Create scan history record
        ScanHistory scanHistory = scanHistoryService.startScan(source);
        ScanTracer.logStep("SOURCE_SCAN", source.getName(), "SCAN_HISTORY_CREATED", 
                "Created scan history record with ID: " + scanHistory.getId());
        
        try {
            // Get the appropriate scanner
            ScanTracer.logStep("SOURCE_SCAN", source.getName(), "GETTING_SCANNER", 
                    "Looking for scanner for source type: " + source.getName());
            
            com.cncf.scanner.scanner.SourceScanner scanner = scannerManager.getScanner(source);
            if (scanner == null) {
                String error = "No scanner available for source: " + source.getName();
                ScanTracer.logError("SOURCE_SCAN", source.getName(), error, 
                        new IllegalStateException(error));
                throw new IllegalStateException(error);
            }
            
            ScanTracer.logStep("SOURCE_SCAN", source.getName(), "SCANNER_FOUND", 
                    "Found scanner: " + scanner.getClass().getSimpleName());
            
            // Get last scan time
            LocalDateTime lastScanTime = scanHistoryService.getLastScanTime(source);
            ScanTracer.logStep("SOURCE_SCAN", source.getName(), "LAST_SCAN_TIME", 
                    "Last scan time: " + (lastScanTime != null ? lastScanTime.toString() : "Never"));
            
            // Perform the scan
            ScanTracer.logStep("SOURCE_SCAN", source.getName(), "STARTING_SCAN", 
                    "Calling scanner.scan() method");
            
            List<ScanResult> scanResults = scanner.scan(source, lastScanTime);
            
            ScanTracer.logDataCollection(source.getName(), "ALL_TOPICS", scanResults.size(), 
                    "Scanner returned " + scanResults.size() + " results");
            
            // Update scan history with results
            scanHistory.setTopicsFound(scanResults.size());
            ScanTracer.logStep("SOURCE_SCAN", source.getName(), "UPDATING_HISTORY", 
                    "Updated scan history with " + scanResults.size() + " topics found");
            
            // Send results to Kafka
            if (!scanResults.isEmpty()) {
                ScanTracer.logStep("SOURCE_SCAN", source.getName(), "SENDING_TO_KAFKA", 
                        "Sending " + scanResults.size() + " topics to Kafka");
                
                topicProducer.sendTopics(source, scanResults);
                scanHistory.setTopicsProcessed(scanResults.size());
                
                ScanTracer.logStep("SOURCE_SCAN", source.getName(), "KAFKA_SENT", 
                        "Successfully sent topics to Kafka");
            } else {
                ScanTracer.logStep("SOURCE_SCAN", source.getName(), "NO_TOPICS", 
                        "No topics found, skipping Kafka send");
            }
            
            // Complete the scan
            scanHistoryService.completeScan(scanHistory);
            
            Duration scanDuration = Duration.between(scanStart, Instant.now());
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("topicsFound", scanResults.size());
            metrics.put("topicsProcessed", scanResults.size());
            metrics.put("scanDurationMs", scanDuration.toMillis());
            metrics.put("lastScanTime", lastScanTime != null ? lastScanTime.toString() : "Never");
            
            ScanTracer.logMetrics("SOURCE_SCAN", source.getName(), metrics);
            
            logger.info("✅ Successfully scanned source {}: found {} topics in {}ms", 
                    source.getName(), scanResults.size(), scanDuration.toMillis());
            
            if (debugConfig.isDetailedScanLogging()) {
                logger.debug("📊 Detailed scan results for {}: {}", source.getName(), 
                        scanResults.stream().map(r -> r.getTitle()).toArray());
            }
            
        } catch (Exception e) {
            Duration scanDuration = Duration.between(scanStart, Instant.now());
            ScanTracer.logError("SOURCE_SCAN", source.getName(), 
                    "Scan failed after " + scanDuration.toMillis() + "ms", e);
            
            logger.error("❌ Error scanning source {} after {}ms: {}", 
                    source.getName(), scanDuration.toMillis(), e.getMessage(), e);
            
            scanHistoryService.failScan(scanHistory, e.getMessage());
            throw e;
        } finally {
            ScanTracer.endTrace("SOURCE_SCAN", source.getName(), 
                    "Scan completed for source: " + source.getName());
        }
    }
    
    /**
     * Scan a specific source manually
     */
    public void scanSourceManually(Long sourceId) {
        Source source = sourceService.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source not found with id: " + sourceId));
        
        scanSource(source);
    }
    
    /**
     * Scan all sources that need scanning
     */
    public void scanSourcesNeedingUpdate() {
        List<Source> sourcesNeedingScan = sourceService.findSourcesNeedingScan();
        
        logger.info("Found {} sources that need scanning", sourcesNeedingScan.size());
        
        for (Source source : sourcesNeedingScan) {
            try {
                scanSource(source);
            } catch (Exception e) {
                logger.error("Error scanning source {}: {}", source.getName(), e.getMessage(), e);
            }
        }
    }
}
