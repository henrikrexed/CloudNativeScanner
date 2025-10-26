package com.cncf.scanner.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

public class TopicMessage {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("source_id")
    private Long sourceId;
    
    @JsonProperty("source_name")
    private String sourceName;
    
    @JsonProperty("external_id")
    private String externalId;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("url")
    private String url;
    
    @JsonProperty("author")
    private String author;
    
    @JsonProperty("interaction_count")
    private Integer interactionCount;
    
    @JsonProperty("view_count")
    private Integer viewCount;
    
    @JsonProperty("score")
    private Integer score;
    
    @JsonProperty("published_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime publishedAt;
    
    @JsonProperty("scanned_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scannedAt;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Constructors
    public TopicMessage() {
        this.scannedAt = LocalDateTime.now();
    }
    
    public TopicMessage(String id, Long sourceId, String sourceName, String externalId, 
                       String title, String content, String url) {
        this();
        this.id = id;
        this.sourceId = sourceId;
        this.sourceName = sourceName;
        this.externalId = externalId;
        this.title = title;
        this.content = content;
        this.url = url;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Long getSourceId() {
        return sourceId;
    }
    
    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }
    
    public String getSourceName() {
        return sourceName;
    }
    
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }
    
    public String getExternalId() {
        return externalId;
    }
    
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public Integer getInteractionCount() {
        return interactionCount;
    }
    
    public void setInteractionCount(Integer interactionCount) {
        this.interactionCount = interactionCount;
    }
    
    public Integer getViewCount() {
        return viewCount;
    }
    
    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }
    
    public Integer getScore() {
        return score;
    }
    
    public void setScore(Integer score) {
        this.score = score;
    }
    
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
    
    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
    
    public LocalDateTime getScannedAt() {
        return scannedAt;
    }
    
    public void setScannedAt(LocalDateTime scannedAt) {
        this.scannedAt = scannedAt;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
