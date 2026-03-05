package com.cncf.scanner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "topic_themes")
@IdClass(TopicThemeId.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "topic"})
public class TopicTheme {
    
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;
    
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "parentTheme", "subThemes", "topicThemes"})
    private Theme theme;
    
    @Column(name = "confidence_score", precision = 3, scale = 2)
    private java.math.BigDecimal confidenceScore = java.math.BigDecimal.ZERO;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Constructors
    public TopicTheme() {}
    
    public TopicTheme(Topic topic, Theme theme, java.math.BigDecimal confidenceScore) {
        this.topic = topic;
        this.theme = theme;
        this.confidenceScore = confidenceScore;
    }
    
    // Getters and Setters
    public Topic getTopic() {
        return topic;
    }
    
    public void setTopic(Topic topic) {
        this.topic = topic;
    }
    
    public Theme getTheme() {
        return theme;
    }
    
    public void setTheme(Theme theme) {
        this.theme = theme;
    }
    
    public java.math.BigDecimal getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(java.math.BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
