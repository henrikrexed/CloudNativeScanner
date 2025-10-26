package com.cncf.scanner.kafka;

import com.cncf.scanner.model.*;
import com.cncf.scanner.service.ClassificationService;
import com.cncf.scanner.service.EnhancedClassificationService;
import com.cncf.scanner.service.ThemeClassification;
import com.cncf.scanner.service.TopicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TopicConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(TopicConsumer.class);
    
    private final TopicService topicService;
    private final ClassificationService classificationService;
    private final EnhancedClassificationService enhancedClassificationService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public TopicConsumer(TopicService topicService, 
                        ClassificationService classificationService,
                        EnhancedClassificationService enhancedClassificationService,
                        ObjectMapper objectMapper) {
        this.topicService = topicService;
        this.classificationService = classificationService;
        this.enhancedClassificationService = enhancedClassificationService;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(topics = "${kafka.topic.name:topic-scanner}", 
                   groupId = "${kafka.consumer.group-id:topic-processor}")
    public void consumeTopicMessage(@Payload String message,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                   @Header(KafkaHeaders.OFFSET) long offset,
                                   Acknowledgment acknowledgment) {
        
        try {
            logger.debug("Received message from topic: {}, partition: {}, offset: {}", 
                    topic, partition, offset);
            
            TopicMessage topicMessage = objectMapper.readValue(message, TopicMessage.class);
            
            // Process the topic with enhanced AI classification
            processTopicMessage(topicMessage);
            
            // Acknowledge the message
            acknowledgment.acknowledge();
            
            logger.debug("Successfully processed topic message: {}", topicMessage.getTitle());
            
        } catch (Exception e) {
            logger.error("Error processing topic message: {}", e.getMessage(), e);
            // In a production environment, you might want to implement retry logic
            // or send to a dead letter queue
        }
    }
    
    private void processTopicMessage(TopicMessage topicMessage) {
        try {
            // Save or update the topic in the database
            Topic topic = saveOrUpdateTopic(topicMessage);
            
            // Use enhanced AI classification
            EnhancedClassificationService.ClassificationResult classificationResult = 
                    enhancedClassificationService.classifyTopic(
                            topicMessage.getTitle(), 
                            topicMessage.getContent()
                    );
            
            // Check if topic should be processed
            if (!classificationResult.isShouldProcess()) {
                if (classificationResult.isDuplicate()) {
                    logger.info("Skipping duplicate topic: {} (similar to: {})", 
                            topicMessage.getTitle(), classificationResult.getSimilarTopicId());
                } else {
                    logger.info("Skipping low-relevance topic: {} (relevance: {})", 
                            topicMessage.getTitle(), classificationResult.getRelevanceScore());
                }
                return;
            }
            
            // Save AI analysis if available
            if (classificationResult.getAiAnalysis() != null) {
                enhancedClassificationService.saveAIAnalysis(topic, classificationResult.getAiAnalysis());
            }
            
            // Save theme classifications
            for (ThemeClassification classification : classificationResult.getThemeClassifications()) {
                topicService.addTopicTheme(topic, classification.getTheme(), 
                        classification.getConfidenceScore());
            }
            
            logger.info("Processed topic: {} with {} theme classifications (AI relevance: {})", 
                    topicMessage.getTitle(), 
                    classificationResult.getThemeClassifications().size(),
                    classificationResult.getRelevanceScore());
            
        } catch (Exception e) {
            logger.error("Error processing topic message: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private Topic saveOrUpdateTopic(TopicMessage topicMessage) {
        // Check if topic already exists
        Topic existingTopic = topicService.findBySourceAndExternalId(
                topicMessage.getSourceId(), 
                topicMessage.getExternalId()
        );
        
        if (existingTopic != null) {
            // Update existing topic
            existingTopic.setTitle(topicMessage.getTitle());
            existingTopic.setContent(topicMessage.getContent());
            existingTopic.setInteractionCount(topicMessage.getInteractionCount());
            existingTopic.setViewCount(topicMessage.getViewCount());
            existingTopic.setScore(topicMessage.getScore());
            existingTopic.setAuthor(topicMessage.getAuthor());
            
            return topicService.save(existingTopic);
        } else {
            // Create new topic
            Source source = new Source();
            source.setId(topicMessage.getSourceId());
            
            Topic newTopic = new Topic();
            newTopic.setSource(source);
            newTopic.setExternalId(topicMessage.getExternalId());
            newTopic.setTitle(topicMessage.getTitle());
            newTopic.setContent(topicMessage.getContent());
            newTopic.setUrl(topicMessage.getUrl());
            newTopic.setAuthor(topicMessage.getAuthor());
            newTopic.setInteractionCount(topicMessage.getInteractionCount());
            newTopic.setViewCount(topicMessage.getViewCount());
            newTopic.setScore(topicMessage.getScore());
            
            return topicService.save(newTopic);
        }
    }
}
