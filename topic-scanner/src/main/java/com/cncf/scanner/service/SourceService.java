package com.cncf.scanner.service;

import com.cncf.scanner.model.Source;
import com.cncf.scanner.repository.SourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SourceService {
    
    private static final Logger logger = LoggerFactory.getLogger(SourceService.class);
    
    private final SourceRepository sourceRepository;
    
    @Autowired
    public SourceService(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }
    
    /**
     * Find all sources
     */
    public List<Source> findAll() {
        return sourceRepository.findAll();
    }
    
    /**
     * Find active sources
     */
    public List<Source> findActiveSources() {
        return sourceRepository.findByIsActiveTrue();
    }
    
    /**
     * Find sources that need scanning
     */
    public List<Source> findSourcesNeedingScan() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24); // Default 24 hours
        return sourceRepository.findSourcesNeedingScan(cutoffTime);
    }
    
    /**
     * Find source by ID
     */
    public Optional<Source> findById(Long id) {
        return sourceRepository.findById(id);
    }
    
    /**
     * Find source by name
     */
    public Source findByName(String name) {
        return sourceRepository.findByName(name).orElse(null);
    }
    
    /**
     * Save or update a source
     */
    public Source save(Source source) {
        return sourceRepository.save(source);
    }
    
    /**
     * Create a new source
     */
    public Source createSource(String name, String baseUrl, String apiEndpoint) {
        if (sourceRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Source with name '" + name + "' already exists");
        }
        
        Source source = new Source(name, baseUrl, apiEndpoint);
        return sourceRepository.save(source);
    }
    
    /**
     * Update source
     */
    public Source updateSource(Long id, String name, String baseUrl, String apiEndpoint, 
                              Boolean isActive, Integer scanFrequencyHours) {
        Source source = sourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Source not found with id: " + id));
        
        source.setName(name);
        source.setBaseUrl(baseUrl);
        source.setApiEndpoint(apiEndpoint);
        source.setIsActive(isActive);
        source.setScanFrequencyHours(scanFrequencyHours);
        
        return sourceRepository.save(source);
    }
    
    /**
     * Activate/deactivate source
     */
    public Source toggleSourceStatus(Long id) {
        Source source = sourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Source not found with id: " + id));
        
        source.setIsActive(!source.getIsActive());
        return sourceRepository.save(source);
    }
    
    /**
     * Delete source
     */
    public void delete(Long id) {
        sourceRepository.deleteById(id);
    }
}
