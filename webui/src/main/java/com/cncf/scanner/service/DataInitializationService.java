package com.cncf.scanner.service;

import com.cncf.scanner.model.Source;
import com.cncf.scanner.model.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DataInitializationService implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializationService.class);
    
    private final SourceService sourceService;
    private final ThemeService themeService;
    
    @Autowired
    public DataInitializationService(SourceService sourceService, ThemeService themeService) {
        this.sourceService = sourceService;
        this.themeService = themeService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        initializeDefaultThemes();
        initializeDefaultSources();
    }
    
    private void initializeDefaultThemes() {
        logger.info("Initializing default themes...");
        
        String[][] defaultThemes = {
            {"Cloud Native", "Topics related to cloud-native technologies, containers, microservices"},
            {"Kubernetes", "Kubernetes-specific discussions, deployments, and configurations"},
            {"DevOps", "DevOps practices, CI/CD, automation, and infrastructure"},
            {"Security", "Security-related topics, vulnerabilities, and best practices"},
            {"Monitoring", "Observability, monitoring, logging, and alerting"},
            {"Development", "General development topics, programming languages, frameworks"},
            {"Architecture", "System design, architecture patterns, and scalability"},
            {"Performance", "Performance optimization, tuning, and benchmarking"}
        };
        
        for (String[] themeData : defaultThemes) {
            String name = themeData[0];
            String description = themeData[1];
            
            if (!themeService.existsByName(name)) {
                themeService.createTheme(name, description);
                logger.info("Created theme: {}", name);
            } else {
                logger.debug("Theme already exists: {}", name);
            }
        }
    }
    
    private void initializeDefaultSources() {
        logger.info("Initializing default sources...");
        
        String[][] defaultSources = {
            {"StackOverflow", "https://stackoverflow.com", "https://api.stackexchange.com/2.3"},
            {"Reddit", "https://reddit.com", "https://www.reddit.com/r"}
        };
        
        for (String[] sourceData : defaultSources) {
            String name = sourceData[0];
            String baseUrl = sourceData[1];
            String apiEndpoint = sourceData[2];
            
            if (sourceService.findByName(name) == null) {
                sourceService.createSource(name, baseUrl, apiEndpoint);
                logger.info("Created source: {}", name);
            } else {
                logger.debug("Source already exists: {}", name);
            }
        }
    }
}


