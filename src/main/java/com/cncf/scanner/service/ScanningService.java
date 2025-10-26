package com.cncf.scanner.service;

import com.cncf.scanner.kafka.TopicProducer;
import com.cncf.scanner.model.*;
import com.cncf.scanner.scanner.ScanResult;
import com.cncf.scanner.scanner.ScannerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    
    @Autowired
    public ScanningService(SourceService sourceService, 
                          ScannerManager scannerManager,
                          TopicProducer topicProducer,
                          ScanHistoryService scanHistoryService) {
        this.sourceService = sourceService;
        this.scannerManager = scannerManager;
        this.topicProducer = topicProducer;
        this.scanHistoryService = scanHistoryService;
    }
    
    /**
     * Scheduled method to run daily scanning
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    public void performDailyScan() {
        logger.info("Starting daily scan of all active sources");
        
        List<Source> activeSources = sourceService.findActiveSources();
        
        for (Source source : activeSources) {
            try {
                scanSource(source);
            } catch (Exception e) {
                logger.error("Error scanning source {}: {}", source.getName(), e.getMessage(), e);
            }
        }
        
        logger.info("Completed daily scan of all active sources");
    }
    
    /**
     * Scan a specific source
     */
    public void scanSource(Source source) {
        logger.info("Starting scan for source: {}", source.getName());
        
        // Create scan history record
        ScanHistory scanHistory = scanHistoryService.startScan(source);
        
        try {
            // Get the appropriate scanner
            com.cncf.scanner.scanner.SourceScanner scanner = scannerManager.getScanner(source);
            if (scanner == null) {
                throw new IllegalStateException("No scanner available for source: " + source.getName());
            }
            
            // Get last scan time
            LocalDateTime lastScanTime = scanHistoryService.getLastScanTime(source);
            
            // Perform the scan
            List<ScanResult> scanResults = scanner.scan(source, lastScanTime);
            
            // Update scan history with results
            scanHistory.setTopicsFound(scanResults.size());
            
            // Send results to Kafka
            if (!scanResults.isEmpty()) {
                topicProducer.sendTopics(source, scanResults);
                scanHistory.setTopicsProcessed(scanResults.size());
            }
            
            // Complete the scan
            scanHistoryService.completeScan(scanHistory);
            
            logger.info("Successfully scanned source {}: found {} topics", 
                    source.getName(), scanResults.size());
            
        } catch (Exception e) {
            logger.error("Error scanning source {}: {}", source.getName(), e.getMessage(), e);
            scanHistoryService.failScan(scanHistory, e.getMessage());
            throw e;
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


