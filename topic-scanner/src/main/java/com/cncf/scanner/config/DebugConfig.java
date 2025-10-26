package com.cncf.scanner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.debug")
public class DebugConfig {
    
    private boolean enabled = true;
    private boolean traceRequests = true;
    private boolean tracePerformance = true;
    private boolean detailedScanLogging = true;
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isTraceRequests() {
        return traceRequests;
    }
    
    public void setTraceRequests(boolean traceRequests) {
        this.traceRequests = traceRequests;
    }
    
    public boolean isTracePerformance() {
        return tracePerformance;
    }
    
    public void setTracePerformance(boolean tracePerformance) {
        this.tracePerformance = tracePerformance;
    }
    
    public boolean isDetailedScanLogging() {
        return detailedScanLogging;
    }
    
    public void setDetailedScanLogging(boolean detailedScanLogging) {
        this.detailedScanLogging = detailedScanLogging;
    }
}

