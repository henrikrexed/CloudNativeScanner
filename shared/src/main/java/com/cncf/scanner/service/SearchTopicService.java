package com.cncf.scanner.service;

import com.cncf.scanner.model.SearchTopic;
import com.cncf.scanner.model.Source;
import com.cncf.scanner.repository.SearchTopicRepository;
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
public class SearchTopicService {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchTopicService.class);
    
    private final SearchTopicRepository searchTopicRepository;
    
    @Autowired
    public SearchTopicService(SearchTopicRepository searchTopicRepository) {
        this.searchTopicRepository = searchTopicRepository;
    }
    
    /**
     * Find all search topics
     */
    public List<SearchTopic> findAll() {
        return searchTopicRepository.findAll();
    }
    
    /**
     * Find search topics by source
     */
    public List<SearchTopic> findBySourceId(Long sourceId) {
        return searchTopicRepository.findBySourceId(sourceId);
    }
    
    /**
     * Find active search topics by source
     */
    public List<SearchTopic> findActiveBySourceId(Long sourceId) {
        return searchTopicRepository.findBySourceIdAndIsActiveTrue(sourceId);
    }
    
    /**
     * Find search topics that need searching
     */
    public List<SearchTopic> findSearchTopicsNeedingSearch(Long sourceId) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24); // Default 24 hours
        return searchTopicRepository.findSearchTopicsNeedingSearch(sourceId, cutoffTime);
    }
    
    /**
     * Find search topic by ID
     */
    public Optional<SearchTopic> findById(Long id) {
        return searchTopicRepository.findById(id);
    }
    
    /**
     * Save or update search topic
     */
    public SearchTopic save(SearchTopic searchTopic) {
        return searchTopicRepository.save(searchTopic);
    }
    
    /**
     * Create a new search topic
     */
    public SearchTopic createSearchTopic(Source source, String keyword, String searchQuery, 
                                       String description, Integer priority, Integer maxResults) {
        SearchTopic searchTopic = new SearchTopic(source, keyword, searchQuery);
        searchTopic.setDescription(description);
        searchTopic.setPriority(priority != null ? priority : 1);
        searchTopic.setMaxResults(maxResults != null ? maxResults : 50);
        
        return searchTopicRepository.save(searchTopic);
    }
    
    /**
     * Update search topic
     */
    public SearchTopic updateSearchTopic(Long id, String keyword, String searchQuery, 
                                       String description, Boolean isActive, Integer priority, 
                                       Integer maxResults, Integer searchFrequencyHours) {
        SearchTopic searchTopic = searchTopicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Search topic not found with id: " + id));
        
        searchTopic.setKeyword(keyword);
        searchTopic.setSearchQuery(searchQuery);
        searchTopic.setDescription(description);
        searchTopic.setIsActive(isActive);
        searchTopic.setPriority(priority);
        searchTopic.setMaxResults(maxResults);
        searchTopic.setSearchFrequencyHours(searchFrequencyHours);
        
        return searchTopicRepository.save(searchTopic);
    }
    
    /**
     * Toggle search topic status
     */
    public SearchTopic toggleSearchTopicStatus(Long id) {
        SearchTopic searchTopic = searchTopicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Search topic not found with id: " + id));
        
        searchTopic.setIsActive(!searchTopic.getIsActive());
        return searchTopicRepository.save(searchTopic);
    }
    
    /**
     * Update last searched timestamp
     */
    public void updateLastSearchedAt(Long id) {
        SearchTopic searchTopic = searchTopicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Search topic not found with id: " + id));
        
        searchTopic.setLastSearchedAt(LocalDateTime.now());
        searchTopicRepository.save(searchTopic);
    }
    
    /**
     * Delete search topic
     */
    public void delete(Long id) {
        searchTopicRepository.deleteById(id);
    }
    
    /**
     * Get search topic statistics
     */
    public SearchTopicStats getSearchTopicStats(Long sourceId) {
        long totalTopics = searchTopicRepository.countBySourceId(sourceId);
        long activeTopics = searchTopicRepository.countBySourceIdAndIsActiveTrue(sourceId);
        
        return new SearchTopicStats(totalTopics, activeTopics);
    }
    
    public static class SearchTopicStats {
        private final long totalTopics;
        private final long activeTopics;
        
        public SearchTopicStats(long totalTopics, long activeTopics) {
            this.totalTopics = totalTopics;
            this.activeTopics = activeTopics;
        }
        
        public long getTotalTopics() { return totalTopics; }
        public long getActiveTopics() { return activeTopics; }
    }
}



