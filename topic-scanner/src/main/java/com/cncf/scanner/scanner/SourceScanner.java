package com.cncf.scanner.scanner;

import com.cncf.scanner.model.Source;
import java.util.List;

public interface SourceScanner {
    
    /**
     * Get the source type this scanner handles
     */
    String getSourceType();
    
    /**
     * Check if this scanner can handle the given source
     */
    boolean canHandle(Source source);
    
    /**
     * Scan the source for new topics
     * @param source The source to scan
     * @param lastScanTime The last time this source was scanned (null for first scan)
     * @return List of scan results
     */
    List<ScanResult> scan(Source source, java.time.LocalDateTime lastScanTime);
    
    /**
     * Get the rate limit for this source (requests per minute)
     */
    default int getRateLimit() {
        return 60; // Default 1 request per second
    }
    
    /**
     * Get the delay between requests in milliseconds
     */
    default long getRequestDelay() {
        return 1000; // Default 1 second
    }
}
