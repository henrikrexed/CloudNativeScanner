package com.cncf.scanner.controller;

import com.cncf.scanner.model.ScanHistory;
import com.cncf.scanner.service.ScanHistoryService;
import com.cncf.scanner.service.TopicService;
import com.cncf.scanner.service.SourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/monitoring")
public class MonitoringController {
    
    private final ScanHistoryService scanHistoryService;
    private final TopicService topicService;
    private final SourceService sourceService;
    
    @Autowired
    public MonitoringController(ScanHistoryService scanHistoryService,
                               TopicService topicService,
                               SourceService sourceService) {
        this.scanHistoryService = scanHistoryService;
        this.topicService = topicService;
        this.sourceService = sourceService;
    }
    
    @GetMapping
    public String monitoringDashboard(Model model) {
        // Get scan statistics
        ScanHistoryService.ScanStatistics stats = scanHistoryService.getScanStatistics();
        model.addAttribute("scanStats", stats);
        
        // Get recent scan history
        List<ScanHistory> recentScans = scanHistoryService.findScansInTimeRange(
                LocalDateTime.now().minusDays(7), LocalDateTime.now());
        model.addAttribute("recentScans", recentScans);
        
        // Get running scans
        List<ScanHistory> runningScans = scanHistoryService.findRunningScans();
        model.addAttribute("runningScans", runningScans);
        
        // Get source statistics
        List<com.cncf.scanner.model.Source> sources = sourceService.findAll();
        model.addAttribute("sources", sources);
        
        return "admin/monitoring";
    }
    
    @GetMapping("/scans")
    public String scanHistory(Model model) {
        List<ScanHistory> allScans = scanHistoryService.findAll();
        model.addAttribute("scans", allScans);
        return "admin/scan-history";
    }
}


