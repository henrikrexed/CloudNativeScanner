package com.cncf.scanner.service;

import com.cncf.scanner.model.TopicAnalysis;
import com.cncf.scanner.repository.TopicAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TopicAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(TopicAnalysisService.class);
    
    private final TopicAnalysisRepository topicAnalysisRepository;
    
    @Autowired
    public TopicAnalysisService(TopicAnalysisRepository topicAnalysisRepository) {
        this.topicAnalysisRepository = topicAnalysisRepository;
    }
    
    /**
     * Save or update topic analysis
     */
    public TopicAnalysis save(TopicAnalysis analysis) {
        return topicAnalysisRepository.save(analysis);
    }
    
    /**
     * Find analysis by topic ID
     */
    public Optional<TopicAnalysis> findByTopicId(Long topicId) {
        return topicAnalysisRepository.findByTopicId(topicId);
    }
    
    /**
     * Find all analyses
     */
    public List<TopicAnalysis> findAll() {
        return topicAnalysisRepository.findAll();
    }
    
    /**
     * Find analyses by relevance score range
     */
    public List<TopicAnalysis> findByRelevanceScoreRange(double minScore, double maxScore) {
        return topicAnalysisRepository.findByRelevanceScoreBetween(minScore, maxScore);
    }
    
    /**
     * Find analyses by complexity level
     */
    public List<TopicAnalysis> findByComplexityLevel(String complexityLevel) {
        return topicAnalysisRepository.findByComplexityLevel(complexityLevel);
    }
    
    /**
     * Delete analysis
     */
    public void delete(Long id) {
        topicAnalysisRepository.deleteById(id);
    }
    
    /**
     * Delete analysis by topic ID
     */
    public void deleteByTopicId(Long topicId) {
        topicAnalysisRepository.deleteByTopicId(topicId);
    }
}


