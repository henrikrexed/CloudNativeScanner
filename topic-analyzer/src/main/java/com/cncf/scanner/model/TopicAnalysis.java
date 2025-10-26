package com.cncf.scanner.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "topic_analysis")
public class TopicAnalysis {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;
    
    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;
    
    @Column(name = "complexity_level")
    private String complexityLevel;
    
    @Column(name = "relevance_score")
    private Double relevanceScore;
    
    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords; // JSON array of keywords
    
    @Column(name = "ai_confidence")
    private Double aiConfidence;
    
    @Column(name = "analysis_version")
    private String analysisVersion = "1.0";
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public TopicAnalysis() {}
    
    public TopicAnalysis(Topic topic) {
        this.topic = topic;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Topic getTopic() {
        return topic;
    }
    
    public void setTopic(Topic topic) {
        this.topic = topic;
    }
    
    public String getAiSummary() {
        return aiSummary;
    }
    
    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }
    
    public String getComplexityLevel() {
        return complexityLevel;
    }
    
    public void setComplexityLevel(String complexityLevel) {
        this.complexityLevel = complexityLevel;
    }
    
    public Double getRelevanceScore() {
        return relevanceScore;
    }
    
    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }
    
    public String getKeywords() {
        return keywords;
    }
    
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
    
    public Double getAiConfidence() {
        return aiConfidence;
    }
    
    public void setAiConfidence(Double aiConfidence) {
        this.aiConfidence = aiConfidence;
    }
    
    public String getAnalysisVersion() {
        return analysisVersion;
    }
    
    public void setAnalysisVersion(String analysisVersion) {
        this.analysisVersion = analysisVersion;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
