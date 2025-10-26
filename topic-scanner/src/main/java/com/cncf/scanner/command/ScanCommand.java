package com.cncf.scanner.command;

import com.cncf.scanner.config.DebugConfig;
import com.cncf.scanner.service.ScanningService;
import com.cncf.scanner.util.ScanTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class ScanCommand implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(ScanCommand.class);
    
    private final ScanningService scanningService;
    private final DebugConfig debugConfig;
    
    @Autowired
    public ScanCommand(ScanningService scanningService, DebugConfig debugConfig) {
        this.scanningService = scanningService;
        this.debugConfig = debugConfig;
    }
    
    @Override
    public void run(String... args) throws Exception {
        ScanTracer.startTrace("COMMAND_LINE_SCAN", "ALL_SOURCES", "Starting command line scanning process");
        Instant processStart = Instant.now();
        
        logger.info("🚀 Starting topic scanning process...");
        
        // Log debug configuration
        if (debugConfig.isEnabled()) {
            logger.info("🔧 Debug mode is ENABLED");
            logger.info("   - Trace requests: {}", debugConfig.isTraceRequests());
            logger.info("   - Trace performance: {}", debugConfig.isTracePerformance());
            logger.info("   - Detailed scan logging: {}", debugConfig.isDetailedScanLogging());
        } else {
            logger.info("🔧 Debug mode is DISABLED");
        }
        
        try {
            ScanTracer.logStep("COMMAND_LINE_SCAN", "ALL_SOURCES", "STARTING_SCAN", 
                    "Calling scanSourcesNeedingUpdate()");
            
            // Scan all sources that need updating
            scanningService.scanSourcesNeedingUpdate();
            
            Duration processDuration = Duration.between(processStart, Instant.now());
            
            ScanTracer.logStep("COMMAND_LINE_SCAN", "ALL_SOURCES", "SCAN_COMPLETED", 
                    "Scan completed successfully in " + processDuration.toMillis() + "ms");
            
            logger.info("✅ Topic scanning process completed successfully in {}ms", processDuration.toMillis());
            
            if (debugConfig.isEnabled()) {
                logger.info("📊 Process Summary:");
                logger.info("   - Total execution time: {}ms", processDuration.toMillis());
                logger.info("   - Debug mode: ENABLED");
                logger.info("   - Check logs for detailed trace information");
            }
            
            System.exit(0);
            
        } catch (Exception e) {
            Duration processDuration = Duration.between(processStart, Instant.now());
            ScanTracer.logError("COMMAND_LINE_SCAN", "ALL_SOURCES", 
                    "Topic scanning process failed after " + processDuration.toMillis() + "ms", e);
            
            logger.error("💥 Topic scanning process failed after {}ms: {}", 
                    processDuration.toMillis(), e.getMessage(), e);
            
            if (debugConfig.isEnabled()) {
                logger.error("🔍 Debug information available in logs above");
            }
            
            System.exit(1);
        } finally {
            ScanTracer.endTrace("COMMAND_LINE_SCAN", "ALL_SOURCES", "Command line scan process completed");
        }
    }
}

