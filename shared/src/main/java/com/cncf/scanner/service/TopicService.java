package com.cncf.scanner.service;

import com.cncf.scanner.model.*;
import com.cncf.scanner.repository.TopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TopicService {
    
    private static final Logger logger = LoggerFactory.getLogger(TopicService.class);
    
    private final TopicRepository topicRepository;
    private final ThemeService themeService;
    
    @Autowired
    public TopicService(TopicRepository topicRepository, ThemeService themeService) {
        this.topicRepository = topicRepository;
        this.themeService = themeService;
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
     * Find topic by ID with eagerly fetched relationships (source and topicThemes)
     * Use this method when you need to serialize the topic to JSON to avoid LazyInitializationException
     */
    @Transactional(readOnly = true)
    public Optional<Topic> findByIdWithRelationships(Long id) {
        return topicRepository.findByIdWithRelationships(id);
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
     * Find all topics (excluding rejected topics)
     */
    public List<Topic> findAll() {
        List<Topic> allTopics = topicRepository.findAll();
        // Filter out rejected topics
        return allTopics.stream()
                .filter(t -> t.getIsRejected() == null || !t.getIsRejected())
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Count all topics
     */
    public long count() {
        return topicRepository.count();
    }
    
    /**
     * Find topics by theme (excluding rejected topics)
     * Note: Repository query already filters rejected topics, but we filter again as defensive programming
     */
    public List<Topic> findByThemeId(Long themeId) {
        logger.debug("Attempting to find topics for theme ID {}", themeId);
        
        // Debug: Check if there are topics with themes at all
        try {
            long totalTopics = topicRepository.count();
            long topicsWithoutThemes = topicRepository.countTopicsWithoutThemes();
            logger.debug("Database stats - Total topics: {}, Topics without themes: {}, Topics with themes: {}", 
                    totalTopics, topicsWithoutThemes, totalTopics - topicsWithoutThemes);
        } catch (Exception e) {
            logger.debug("Could not get topic statistics: {}", e.getMessage());
        }
        
        List<Topic> topics = topicRepository.findByThemeId(themeId);
        if (topics == null) {
            logger.debug("No topics found for theme ID {} (repository returned null)", themeId);
            return List.of();
        }
        
        logger.debug("Repository returned {} topics for theme ID {} before filtering", topics.size(), themeId);
        
        // Filter out rejected topics (defensive - repository already does this)
        List<Topic> filteredTopics = topics.stream()
                .filter(t -> t.getIsRejected() == null || !t.getIsRejected())
                .collect(java.util.stream.Collectors.toList());
        
        logger.debug("Found {} topics for theme ID {} ({} after filtering rejected)", 
                topics.size(), themeId, filteredTopics.size());
        
        // Log some details about the topics found (if any)
        if (!filteredTopics.isEmpty()) {
            logger.debug("Sample topics for theme {}: {}", themeId, 
                    filteredTopics.stream()
                            .limit(3)
                            .map(t -> String.format("ID=%d, Title=%s", t.getId(), t.getTitle()))
                            .collect(java.util.stream.Collectors.joining(", ")));
        } else {
            logger.warn("⚠️ No topics found for theme ID {} - topics may not be linked to this theme or its sub-themes", themeId);
        }
        
        return filteredTopics;
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
     * Reloads the topic to ensure it's a managed entity, which is required for cascade to work properly
     */
    public void addTopicTheme(Topic topic, Theme theme, Double confidenceScore) {
        // Reload topic to ensure we have a managed entity (required for cascade to work)
        Topic managedTopic = topicRepository.findById(topic.getId())
                .orElseThrow(() -> new RuntimeException("Topic not found: " + topic.getId()));
        
        // Reload theme to ensure it's a managed entity
        Theme managedTheme = themeService.findById(theme.getId())
                .orElseThrow(() -> new RuntimeException("Theme not found: " + theme.getId()));
        
        // Initialize topicThemes Set if it's null
        if (managedTopic.getTopicThemes() == null) {
            managedTopic.setTopicThemes(new java.util.HashSet<>());
        }
        
        // Check if this theme is already associated with the topic
        boolean alreadyExists = managedTopic.getTopicThemes().stream()
                .anyMatch(tt -> tt.getTheme() != null && tt.getTheme().getId().equals(managedTheme.getId()));
        
        if (alreadyExists) {
            logger.debug("Theme {} already associated with topic {} - skipping", 
                    managedTheme.getName(), managedTopic.getId());
            return;
        }
        
        TopicTheme topicTheme = new TopicTheme(managedTopic, managedTheme, 
            confidenceScore != null ? java.math.BigDecimal.valueOf(confidenceScore) : java.math.BigDecimal.ZERO);
        managedTopic.getTopicThemes().add(topicTheme);
        
        // Save the topic (cascade will save the TopicTheme)
        Topic savedTopic = topicRepository.save(managedTopic);
        
        // Force flush to ensure TopicTheme is persisted immediately
        topicRepository.flush();
        
        logger.info("✅ Added theme '{}' (ID: {}) to topic '{}' (ID: {}) with confidence {}. Total themes for topic: {}", 
                managedTheme.getName(), managedTheme.getId(), savedTopic.getTitle(), savedTopic.getId(), 
                confidenceScore, savedTopic.getTopicThemes().size());
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
     * Count topics by content extraction status
     */
    public long countByContentExtractionStatus(String status) {
        return topicRepository.countByContentExtractionStatus(status);
    }
    
    /**
     * Count topics by source and content extraction status
     */
    public long countBySourceIdAndContentExtractionStatus(Long sourceId, String status) {
        return topicRepository.countBySourceIdAndContentExtractionStatus(sourceId, status);
    }
    
    /**
     * Count topics with content summary (stored in RAG)
     */
    public long countTopicsWithSummary() {
        return topicRepository.countTopicsWithSummary();
    }
    
    /**
     * Count topics in RAG (clustered topics)
     */
    public long countTopicsInRAG() {
        return topicRepository.countTopicsInRAG();
    }
    
    /**
     * Count topics without any theme classifications
     */
    public long countTopicsWithoutThemes() {
        return topicRepository.countTopicsWithoutThemes();
    }
    
    /**
     * Check if a topic has any theme classifications
     */
    public boolean hasTopicThemes(Long topicId) {
        return topicRepository.hasTopicThemes(topicId);
    }
    
    /**
     * Count topics created after a specific date
     */
    public long countTopicsCreatedAfter(java.time.LocalDateTime since) {
        return topicRepository.countTopicsCreatedAfter(since);
    }
    
    /**
     * Find hottest topics sorted by engagement score
     */
    public List<Topic> findHottestTopics() {
        return topicRepository.findHottestTopics();
    }
    
    /**
     * Find related topics by title similarity
     * Extracts keywords from the title and finds topics with similar titles from different sources
     */
    public List<Topic> findRelatedTopics(Long topicId, String title) {
        if (title == null || title.trim().isEmpty()) {
            return List.of();
        }
        
        // Extract key words from title (remove common words, take first 3-5 significant words)
        String[] words = title.toLowerCase().split("\\s+");
        // Filter out common words and take meaningful keywords
        java.util.Set<String> stopWords = java.util.Set.of("how", "to", "what", "is", "the", "a", "an", "and", "or", "but", "in", "on", "at", "for", "with", "about");
        String keywords = java.util.Arrays.stream(words)
                .filter(word -> word.length() > 3 && !stopWords.contains(word))
                .limit(3)
                .collect(java.util.stream.Collectors.joining(" "));
        
        if (keywords.isEmpty()) {
            // Fallback: use first word if no keywords found
            keywords = words.length > 0 ? words[0] : "";
        }
        
        if (keywords.isEmpty()) {
            return List.of();
        }
        
        return topicRepository.findRelatedTopicsByTitle(keywords, topicId);
    }
    
    /**
     * Find topics by sub-theme (child theme)
     */
    public List<Topic> findBySubThemeId(Long subThemeId) {
        return topicRepository.findBySubThemeId(subThemeId);
    }
    
    /**
     * Find hottest topics by sub-theme (sorted by interactions/engagement)
     */
    public List<Topic> findHottestTopicsBySubTheme(Long subThemeId) {
        return topicRepository.findHottestTopicsBySubTheme(subThemeId);
    }
    
    /**
     * Find topics by cluster/group ID (optimized - uses database query)
     */
    @Transactional(readOnly = true)
    public List<Topic> findByTopicGroupId(Long groupId) {
        return topicRepository.findByTopicGroupId(groupId);
    }
    
    /**
     * Find all distinct cluster IDs
     */
    @Transactional(readOnly = true)
    public List<Long> findAllClusterIds() {
        return topicRepository.findAllClusterIds();
    }
    
    /**
     * Find cluster IDs filtered by theme
     */
    @Transactional(readOnly = true)
    public List<Long> findClusterIdsByThemeId(Long themeId) {
        return topicRepository.findClusterIdsByThemeId(themeId);
    }
    
    /**
     * Count topics in a cluster
     */
    @Transactional(readOnly = true)
    public long countByTopicGroupId(Long groupId) {
        return topicRepository.countByTopicGroupId(groupId);
    }
    
    /**
     * Find representative topic for a cluster (highest engagement)
     */
    @Transactional(readOnly = true)
    public Optional<Topic> findRepresentativeTopicByGroupId(Long groupId) {
        List<Topic> topics = topicRepository.findRepresentativeTopicByGroupId(
                groupId, 
                PageRequest.of(0, 1)
        );
        return topics.isEmpty() ? Optional.empty() : Optional.of(topics.get(0));
    }
    
    /**
     * Find sample topics in a cluster for tag generation (optimized - limits at DB level)
     */
    public List<Topic> findSampleTopicsByGroupId(Long groupId, int limit) {
        return topicRepository.findSampleTopicsByGroupId(groupId, PageRequest.of(0, limit));
    }
    
    /**
     * Find topics by theme with database-level pagination
     */
    public Page<Topic> findByThemeIdPaginated(Long themeId, Pageable pageable) {
        return topicRepository.findByThemeIdPaginated(themeId, pageable);
    }
    
    /**
     * Find hot topics that need conversation collection
     */
    public List<Topic> findHotTopicsNeedingProcessing() {
        return topicRepository.findHotTopicsNeedingProcessing();
    }
    
    /**
     * Find all hot topics
     */
    public List<Topic> findAllHotTopics() {
        return topicRepository.findAllHotTopics();
    }
    
    /**
     * Delete topic
     */
    public void delete(Long id) {
        topicRepository.deleteById(id);
    }
}
