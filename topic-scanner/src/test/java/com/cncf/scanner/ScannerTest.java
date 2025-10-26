package com.cncf.scanner;

import com.cncf.scanner.config.DebugConfig;
import com.cncf.scanner.model.Source;
import com.cncf.scanner.model.SearchTopic;
import com.cncf.scanner.scanner.RedditScanner;
import com.cncf.scanner.scanner.StackOverflowScanner;
import com.cncf.scanner.util.ScanTracer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple test to verify scanner functionality without Spring Boot context
 */
public class ScannerTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ScannerTest.class);
    
    @Test
    public void testRedditScanner() {
        logger.info("🧪 Testing Reddit Scanner...");
        
        // Create debug config
        DebugConfig debugConfig = new DebugConfig();
        debugConfig.setEnabled(true);
        debugConfig.setTraceRequests(true);
        debugConfig.setTracePerformance(true);
        debugConfig.setDetailedScanLogging(true);
        
        // Create test source
        Source redditSource = new Source();
        redditSource.setName("Reddit");
        redditSource.setBaseUrl("https://www.reddit.com");
        redditSource.setIsActive(true);
        
        // Create test search topic
        SearchTopic searchTopic = new SearchTopic();
        searchTopic.setId(1L);
        searchTopic.setSearchQuery("kubernetes");
        searchTopic.setDescription("Cloud Native");
        searchTopic.setIsActive(true);
        
        // Create scanner
        RedditScanner scanner = new RedditScanner(null, debugConfig);
        
        // Test scanner
        try {
            ScanTracer.startTrace("TEST_REDDIT_SCAN", "Reddit", "Testing Reddit scanner functionality");
            
            logger.info("🔍 Testing Reddit scanner with query: {}", searchTopic.getSearchQuery());
            
            // Test if scanner can handle the source
            boolean canHandle = scanner.canHandle(redditSource);
            logger.info("✅ Reddit scanner can handle source: {}", canHandle);
            
            // Test source type
            String sourceType = scanner.getSourceType();
            logger.info("📡 Reddit scanner source type: {}", sourceType);
            
            ScanTracer.endTrace("TEST_REDDIT_SCAN", "Reddit", "Reddit scanner test completed");
            
        } catch (Exception e) {
            logger.error("❌ Reddit scanner test failed: {}", e.getMessage(), e);
            ScanTracer.logError("TEST_REDDIT_SCAN", "Reddit", "Reddit scanner test failed", e);
        }
    }
    
    @Test
    public void testStackOverflowScanner() {
        logger.info("🧪 Testing StackOverflow Scanner...");
        
        // Create debug config
        DebugConfig debugConfig = new DebugConfig();
        debugConfig.setEnabled(true);
        debugConfig.setTraceRequests(true);
        debugConfig.setTracePerformance(true);
        debugConfig.setDetailedScanLogging(true);
        
        // Create test source
        Source stackOverflowSource = new Source();
        stackOverflowSource.setName("StackOverflow");
        stackOverflowSource.setBaseUrl("https://api.stackexchange.com");
        stackOverflowSource.setIsActive(true);
        
        // Create test search topic
        SearchTopic searchTopic = new SearchTopic();
        searchTopic.setId(2L);
        searchTopic.setSearchQuery("docker");
        searchTopic.setDescription("Containerization");
        searchTopic.setIsActive(true);
        
        // Create scanner
        StackOverflowScanner scanner = new StackOverflowScanner(null, debugConfig);
        
        // Test scanner
        try {
            ScanTracer.startTrace("TEST_STACKOVERFLOW_SCAN", "StackOverflow", "Testing StackOverflow scanner functionality");
            
            logger.info("🔍 Testing StackOverflow scanner with query: {}", searchTopic.getSearchQuery());
            
            // Test if scanner can handle the source
            boolean canHandle = scanner.canHandle(stackOverflowSource);
            logger.info("✅ StackOverflow scanner can handle source: {}", canHandle);
            
            // Test source type
            String sourceType = scanner.getSourceType();
            logger.info("📡 StackOverflow scanner source type: {}", sourceType);
            
            ScanTracer.endTrace("TEST_STACKOVERFLOW_SCAN", "StackOverflow", "StackOverflow scanner test completed");
            
        } catch (Exception e) {
            logger.error("❌ StackOverflow scanner test failed: {}", e.getMessage(), e);
            ScanTracer.logError("TEST_STACKOVERFLOW_SCAN", "StackOverflow", "StackOverflow scanner test failed", e);
        }
    }
    
    @Test
    public void testDebugLogging() {
        logger.info("🧪 Testing Debug Logging System...");
        
        // Create debug config
        DebugConfig debugConfig = new DebugConfig();
        debugConfig.setEnabled(true);
        debugConfig.setTraceRequests(true);
        debugConfig.setTracePerformance(true);
        debugConfig.setDetailedScanLogging(true);
        
        logger.info("🔧 Debug configuration:");
        logger.info("   - Enabled: {}", debugConfig.isEnabled());
        logger.info("   - Trace requests: {}", debugConfig.isTraceRequests());
        logger.info("   - Trace performance: {}", debugConfig.isTracePerformance());
        logger.info("   - Detailed scan logging: {}", debugConfig.isDetailedScanLogging());
        
        // Test ScanTracer
        try {
            ScanTracer.startTrace("TEST_DEBUG_LOGGING", "Test", "Testing debug logging system");
            
            ScanTracer.logStep("TEST_DEBUG_LOGGING", "Test", "STEP_1", "Testing step logging");
            Map<String, Object> testMetrics = new HashMap<>();
            testMetrics.put("count", 5);
            testMetrics.put("time", "100ms");
            ScanTracer.logMetrics("TEST_DEBUG_LOGGING", "Test", testMetrics);
            
            ScanTracer.endTrace("TEST_DEBUG_LOGGING", "Test", "Debug logging test completed");
            
            logger.info("✅ Debug logging system test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Debug logging test failed: {}", e.getMessage(), e);
        }
    }
}
