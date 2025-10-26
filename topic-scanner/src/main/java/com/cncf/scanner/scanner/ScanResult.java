package com.cncf.scanner.scanner;

import java.time.LocalDateTime;
import java.util.Map;

public class ScanResult {
    
    private String externalId;
    private String title;
    private String content;
    private String url;
    private String author;
    private Integer interactionCount;
    private Integer viewCount;
    private Integer score;
    private LocalDateTime publishedAt;
    private Map<String, Object> metadata;
    
    // Constructors
    public ScanResult() {}
    
    public ScanResult(String externalId, String title, String url) {
        this.externalId = externalId;
        this.title = title;
        this.url = url;
    }
    
    // Getters and Setters
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
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
