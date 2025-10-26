package com.cncf.scanner.scanner;

import com.cncf.scanner.model.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ScannerManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ScannerManager.class);
    
    private final Map<String, SourceScanner> scanners;
    
    @Autowired
    public ScannerManager(List<SourceScanner> scannerList) {
        this.scanners = scannerList.stream()
                .collect(Collectors.toMap(
                        SourceScanner::getSourceType,
                        Function.identity()
                ));
        
        logger.info("Initialized {} source scanners: {}", 
                scanners.size(), scanners.keySet());
    }
    
    /**
     * Get the appropriate scanner for a source
     */
    public SourceScanner getScanner(Source source) {
        SourceScanner scanner = scanners.get(source.getName());
        if (scanner == null) {
            logger.warn("No scanner found for source: {}", source.getName());
            return null;
        }
        
        if (!scanner.canHandle(source)) {
            logger.warn("Scanner {} cannot handle source {}", 
                    scanner.getSourceType(), source.getName());
            return null;
        }
        
        return scanner;
    }
    
    /**
     * Get all available scanner types
     */
    public List<String> getAvailableScannerTypes() {
        return scanners.keySet().stream().collect(Collectors.toList());
    }
    
    /**
     * Check if a scanner is available for the given source type
     */
    public boolean hasScanner(String sourceType) {
        return scanners.containsKey(sourceType);
    }
}
