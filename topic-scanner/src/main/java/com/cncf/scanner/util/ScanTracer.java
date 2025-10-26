package com.cncf.scanner.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Utility class for tracing scan operations and performance monitoring
 */
public class ScanTracer {
    
    private static final Logger logger = LoggerFactory.getLogger(ScanTracer.class);
    private static final Map<String, Instant> activeTraces = new ConcurrentHashMap<>();
    
    /**
     * Start a trace for a specific operation
     */
    public static void startTrace(String operation, String sourceName, String details) {
        String traceId = generateTraceId(operation, sourceName);
        Instant startTime = Instant.now();
        activeTraces.put(traceId, startTime);
        
        MDC.put("traceId", traceId);
        MDC.put("operation", operation);
        MDC.put("source", sourceName);
        
        logger.debug("🔍 Starting trace: {} for source: {} - {}", operation, sourceName, details);
    }
    
    /**
     * End a trace and log the duration
     */
    public static void endTrace(String operation, String sourceName, String result) {
        String traceId = generateTraceId(operation, sourceName);
        Instant startTime = activeTraces.remove(traceId);
        
        if (startTime != null) {
            Duration duration = Duration.between(startTime, Instant.now());
            logger.debug("✅ Completed trace: {} for source: {} in {}ms - {}", 
                    operation, sourceName, duration.toMillis(), result);
        }
        
        // Clear MDC for this thread
        MDC.clear();
    }
    
    /**
     * Log a step within a trace
     */
    public static void logStep(String operation, String sourceName, String step, String details) {
        String traceId = generateTraceId(operation, sourceName);
        MDC.put("traceId", traceId);
        MDC.put("operation", operation);
        MDC.put("source", sourceName);
        MDC.put("step", step);
        
        logger.debug("📋 Step: {} - {}", step, details);
    }
    
    /**
     * Log an error within a trace
     */
    public static void logError(String operation, String sourceName, String error, Exception e) {
        String traceId = generateTraceId(operation, sourceName);
        MDC.put("traceId", traceId);
        MDC.put("operation", operation);
        MDC.put("source", sourceName);
        
        logger.error("❌ Error in {} for source {}: {} - {}", operation, sourceName, error, e.getMessage(), e);
    }
    
    /**
     * Log performance metrics
     */
    public static void logMetrics(String operation, String sourceName, Map<String, Object> metrics) {
        String traceId = generateTraceId(operation, sourceName);
        MDC.put("traceId", traceId);
        MDC.put("operation", operation);
        MDC.put("source", sourceName);
        
        StringBuilder metricsStr = new StringBuilder("📊 Metrics: ");
        metrics.forEach((key, value) -> metricsStr.append(key).append("=").append(value).append(" "));
        
        logger.debug(metricsStr.toString());
    }
    
    /**
     * Generate a unique trace ID
     */
    private static String generateTraceId(String operation, String sourceName) {
        return String.format("%s-%s-%d", operation, sourceName, Thread.currentThread().getId());
    }
    
    /**
     * Log HTTP request details
     */
    public static void logHttpRequest(String method, String url, Map<String, String> headers, String body) {
        logger.debug("🌐 HTTP Request: {} {} - Headers: {} - Body: {}", method, url, headers, body);
    }
    
    /**
     * Log HTTP response details
     */
    public static void logHttpResponse(String url, int statusCode, String responseBody, long responseTimeMs) {
        logger.debug("📡 HTTP Response: {} - Status: {} - Time: {}ms - Body: {}", 
                url, statusCode, responseTimeMs, responseBody);
    }
    
    /**
     * Log data collection details
     */
    public static void logDataCollection(String sourceName, String topic, int itemsFound, String details) {
        logger.debug("📥 Data Collection: Source={} Topic={} Items={} - {}", 
                sourceName, topic, itemsFound, details);
    }
    
    /**
     * Log algorithm flow
     */
    public static void logAlgorithmFlow(String sourceName, String step, String input, String output) {
        logger.debug("🔄 Algorithm Flow: Source={} Step={} Input={} Output={}", 
                sourceName, step, input, output);
    }
}

