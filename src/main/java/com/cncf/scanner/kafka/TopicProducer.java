package com.cncf.scanner.kafka;

import com.cncf.scanner.model.Source;
import com.cncf.scanner.scanner.ScanResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class TopicProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(TopicProducer.class);
    
    @Value("${kafka.topic.name:topic-scanner}")
    private String topicName;
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public TopicProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Send a scan result to Kafka topic
     */
    public CompletableFuture<SendResult<String, String>> sendTopic(Source source, ScanResult scanResult) {
        try {
            TopicMessage message = createTopicMessage(source, scanResult);
            String messageJson = objectMapper.writeValueAsString(message);
            
            String key = String.format("%s-%s", source.getId(), scanResult.getExternalId());
            
            logger.debug("Sending topic message to Kafka: {}", message.getTitle());
            
            return kafkaTemplate.send(topicName, key, messageJson)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.debug("Successfully sent topic message: {}", message.getTitle());
                        } else {
                            logger.error("Failed to send topic message: {}", message.getTitle(), ex);
                        }
                    });
                    
        } catch (JsonProcessingException e) {
            logger.error("Error serializing topic message: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Send multiple scan results to Kafka topic
     */
    public void sendTopics(Source source, java.util.List<ScanResult> scanResults) {
        logger.info("Sending {} topics to Kafka for source: {}", scanResults.size(), source.getName());
        
        scanResults.parallelStream().forEach(scanResult -> {
            try {
                sendTopic(source, scanResult).get();
            } catch (Exception e) {
                logger.error("Error sending topic {}: {}", scanResult.getTitle(), e.getMessage(), e);
            }
        });
        
        logger.info("Completed sending topics to Kafka for source: {}", source.getName());
    }
    
    private TopicMessage createTopicMessage(Source source, ScanResult scanResult) {
        TopicMessage message = new TopicMessage();
        
        message.setId(UUID.randomUUID().toString());
        message.setSourceId(source.getId());
        message.setSourceName(source.getName());
        message.setExternalId(scanResult.getExternalId());
        message.setTitle(scanResult.getTitle());
        message.setContent(scanResult.getContent());
        message.setUrl(scanResult.getUrl());
        message.setAuthor(scanResult.getAuthor());
        message.setInteractionCount(scanResult.getInteractionCount());
        message.setViewCount(scanResult.getViewCount());
        message.setScore(scanResult.getScore());
        message.setPublishedAt(scanResult.getPublishedAt());
        message.setMetadata(scanResult.getMetadata());
        
        return message;
    }
}


