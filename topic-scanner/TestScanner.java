import com.cncf.scanner.config.DebugConfig;
import com.cncf.scanner.model.Source;
import com.cncf.scanner.model.SearchTopic;
import com.cncf.scanner.scanner.RedditScanner;
import com.cncf.scanner.scanner.StackOverflowScanner;
import com.cncf.scanner.util.ScanTracer;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple standalone test to verify scanner functionality
 * Run with: java -cp target/classes:target/test-classes TestScanner
 */
public class TestScanner {
    
    public static void main(String[] args) {
        System.out.println("🧪 Starting Scanner Test...");
        
        // Test debug logging
        testDebugLogging();
        
        // Test Reddit scanner
        testRedditScanner();
        
        // Test StackOverflow scanner
        testStackOverflowScanner();
        
        System.out.println("✅ Scanner tests completed!");
    }
    
    private static void testDebugLogging() {
        System.out.println("\n🔧 Testing Debug Logging System...");
        
        // Create debug config
        DebugConfig debugConfig = new DebugConfig();
        debugConfig.setEnabled(true);
        debugConfig.setTraceRequests(true);
        debugConfig.setTracePerformance(true);
        debugConfig.setDetailedScanLogging(true);
        
        System.out.println("   - Debug enabled: " + debugConfig.isEnabled());
        System.out.println("   - Trace requests: " + debugConfig.isTraceRequests());
        System.out.println("   - Trace performance: " + debugConfig.isTracePerformance());
        System.out.println("   - Detailed scan logging: " + debugConfig.isDetailedScanLogging());
        
        // Test ScanTracer
        try {
            ScanTracer.startTrace("TEST_DEBUG_LOGGING", "Test", "Testing debug logging system");
            
            ScanTracer.logStep("TEST_DEBUG_LOGGING", "Test", "STEP_1", "Testing step logging");
            Map<String, Object> testMetrics = new HashMap<>();
            testMetrics.put("count", 5);
            testMetrics.put("time", "100ms");
            ScanTracer.logMetrics("TEST_DEBUG_LOGGING", "Test", testMetrics);
            
            ScanTracer.endTrace("TEST_DEBUG_LOGGING", "Test", "Debug logging test completed");
            
            System.out.println("✅ Debug logging system test completed successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Debug logging test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testRedditScanner() {
        System.out.println("\n🔍 Testing Reddit Scanner...");
        
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
            
            System.out.println("   - Testing Reddit scanner with query: " + searchTopic.getSearchQuery());
            
            // Test if scanner can handle the source
            boolean canHandle = scanner.canHandle(redditSource);
            System.out.println("   - Reddit scanner can handle source: " + canHandle);
            
            // Test source type
            String sourceType = scanner.getSourceType();
            System.out.println("   - Reddit scanner source type: " + sourceType);
            
            ScanTracer.endTrace("TEST_REDDIT_SCAN", "Reddit", "Reddit scanner test completed");
            
            System.out.println("✅ Reddit scanner test completed successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Reddit scanner test failed: " + e.getMessage());
            e.printStackTrace();
            ScanTracer.logError("TEST_REDDIT_SCAN", "Reddit", "Reddit scanner test failed", e);
        }
    }
    
    private static void testStackOverflowScanner() {
        System.out.println("\n🔍 Testing StackOverflow Scanner...");
        
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
            
            System.out.println("   - Testing StackOverflow scanner with query: " + searchTopic.getSearchQuery());
            
            // Test if scanner can handle the source
            boolean canHandle = scanner.canHandle(stackOverflowSource);
            System.out.println("   - StackOverflow scanner can handle source: " + canHandle);
            
            // Test source type
            String sourceType = scanner.getSourceType();
            System.out.println("   - StackOverflow scanner source type: " + sourceType);
            
            ScanTracer.endTrace("TEST_STACKOVERFLOW_SCAN", "StackOverflow", "StackOverflow scanner test completed");
            
            System.out.println("✅ StackOverflow scanner test completed successfully");
            
        } catch (Exception e) {
            System.err.println("❌ StackOverflow scanner test failed: " + e.getMessage());
            e.printStackTrace();
            ScanTracer.logError("TEST_STACKOVERFLOW_SCAN", "StackOverflow", "StackOverflow scanner test failed", e);
        }
    }
}
