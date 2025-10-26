package com.cncf.scanner.service;

import com.cncf.scanner.model.*;
import com.cncf.scanner.repository.TopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TopicService {
    
    private static final Logger logger = LoggerFactory.getLogger(TopicService.class);
    
    private final TopicRepository topicRepository;
    
    @Autowired
    public TopicService(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }
    
    /**
     * Save or update a topic
     */
    public Topic save(Topic topic) {
        return topicRepository.save(topic);
    }
    
    /**
     * Find topic by ID
     */
    public Optional<Topic> findById(Long id) {
        return topicRepository.findById(id);
    }
    
    /**
     * Find topic by source and external ID
     */
    public Topic findBySourceAndExternalId(Long sourceId, String externalId) {
        return topicRepository.findBySourceIdAndExternalId(sourceId, externalId).orElse(null);
    }
    
    /**
     * Check if topic exists
     */
    public boolean existsBySourceAndExternalId(Long sourceId, String externalId) {
        return topicRepository.findBySourceIdAndExternalId(sourceId, externalId).isPresent();
    }
    
    /**
     * Find topics by source
     */
    public List<Topic> findBySourceId(Long sourceId) {
        return topicRepository.findBySourceId(sourceId);
    }
    
    /**
     * Find topics by theme
     */
    public List<Topic> findByThemeId(Long themeId) {
        return topicRepository.findByThemeId(themeId);
    }
    
    /**
     * Find recent topics
     */
    public List<Topic> findRecentTopics() {
        return topicRepository.findRecentTopics();
    }
    
    /**
     * Find popular topics
     */
    public List<Topic> findPopularTopics(Integer minInteractions) {
        return topicRepository.findPopularTopics(minInteractions);
    }
    
    /**
     * Add a theme classification to a topic
     */
    public void addTopicTheme(Topic topic, Theme theme, Double confidenceScore) {
        TopicTheme topicTheme = new TopicTheme(topic, theme, confidenceScore);
        topic.getTopicThemes().add(topicTheme);
        topicRepository.save(topic);
        
        logger.debug("Added theme {} to topic {} with confidence {}", 
                theme.getName(), topic.getTitle(), confidenceScore);
    }
    
    /**
     * Remove a theme classification from a topic
     */
    public void removeTopicTheme(Topic topic, Theme theme) {
        topic.getTopicThemes().removeIf(tt -> tt.getTheme().getId().equals(theme.getId()));
        topicRepository.save(topic);
        
        logger.debug("Removed theme {} from topic {}", theme.getName(), topic.getTitle());
    }
    
    /**
     * Get topic statistics
     */
    public long countBySourceId(Long sourceId) {
        return topicRepository.countBySourceId(sourceId);
    }
    
    /**
     * Delete topic
     */
    public void delete(Long id) {
        topicRepository.deleteById(id);
    }
}
