package com.cncf.scanner.service;

import com.cncf.scanner.model.ScanHistory;
import com.cncf.scanner.model.Source;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScanHistoryService {

    public List<ScanHistory> findBySource(Source source) {
        // Mock implementation for testing
        return List.of();
    }

    public ScanHistory save(ScanHistory scanHistory) {
        // Mock implementation for testing
        return scanHistory;
    }

    public LocalDateTime getLastScanTime(Source source) {
        // Mock implementation for testing
        return LocalDateTime.now().minusHours(24);
    }

    public ScanHistory startScan(Source source) {
        // Mock implementation for testing
        ScanHistory scanHistory = new ScanHistory();
        scanHistory.setSource(source);
        scanHistory.setStartedAt(LocalDateTime.now());
        scanHistory.setStatus("RUNNING");
        return scanHistory;
    }

    public ScanHistory completeScan(ScanHistory scanHistory) {
        // Mock implementation for testing
        scanHistory.setCompletedAt(LocalDateTime.now());
        scanHistory.setStatus("COMPLETED");
        return scanHistory;
    }

    public ScanHistory failScan(ScanHistory scanHistory, String errorMessage) {
        // Mock implementation for testing
        scanHistory.setCompletedAt(LocalDateTime.now());
        scanHistory.setStatus("FAILED");
        scanHistory.setErrorMessage(errorMessage);
        return scanHistory;
    }
}
