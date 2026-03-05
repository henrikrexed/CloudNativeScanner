package com.cncf.scanner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a learned pattern from user feedback
 * Used to improve scanner behavior by avoiding or prioritizing certain content
 */
@Entity
@Table(name = "feedback_patterns")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FeedbackPattern {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "pattern_type", nullable = false, length = 50)
    private String patternType; // "AVOID" or "PRIORITIZE"
    
    @Column(name = "pattern_text", nullable = false, columnDefinition = "TEXT")
    private String patternText; // The keyword, phrase, or pattern
    
    @Column(name = "pattern_category", length = 100)
    private String patternCategory; // "KEYWORD", "PHRASE", "TITLE_PATTERN", "CONTENT_PATTERN"
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Source source; // Optional: pattern specific to a source
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Theme theme; // Optional: pattern specific to a theme
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason; // For AVOID patterns: why it was rejected
    
    @Column(name = "confidence_score", precision = 3, scale = 2)
    private java.math.BigDecimal confidenceScore = java.math.BigDecimal.ONE;
    
    @Column(name = "usage_count")
    private Integer usageCount = 0;
    
    @Column(name = "success_count")
    private Integer successCount = 0;
    
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
    public FeedbackPattern() {}
    
    public FeedbackPattern(String patternType, String patternText, String patternCategory) {
        this.patternType = patternType;
        this.patternText = patternText;
        this.patternCategory = patternCategory;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getPatternType() {
        return patternType;
    }
    
    public void setPatternType(String patternType) {
        this.patternType = patternType;
    }
    
    public String getPatternText() {
        return patternText;
    }
    
    public void setPatternText(String patternText) {
        this.patternText = patternText;
    }
    
    public String getPatternCategory() {
        return patternCategory;
    }
    
    public void setPatternCategory(String patternCategory) {
        this.patternCategory = patternCategory;
    }
    
    public Source getSource() {
        return source;
    }
    
    public void setSource(Source source) {
        this.source = source;
    }
    
    public Theme getTheme() {
        return theme;
    }
    
    public void setTheme(Theme theme) {
        this.theme = theme;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    public java.math.BigDecimal getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(java.math.BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
    
    public Integer getUsageCount() {
        return usageCount != null ? usageCount : 0;
    }
    
    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }
    
    public Integer getSuccessCount() {
        return successCount != null ? successCount : 0;
    }
    
    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
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
    
    /**
     * Increment usage count and update confidence based on success
     */
    public void recordUsage(boolean wasSuccessful) {
        this.usageCount++;
        if (wasSuccessful) {
            this.successCount++;
        }
        // Update confidence: success rate
        if (this.usageCount > 0) {
            double successRate = (double) this.successCount / this.usageCount;
            this.confidenceScore = java.math.BigDecimal.valueOf(successRate);
        }
    }
    
    /**
     * Check if this pattern matches the given text
     */
    public boolean matches(String text) {
        if (text == null || patternText == null) {
            return false;
        }
        String textLower = text.toLowerCase();
        String patternLower = patternText.toLowerCase();
        
        // Simple contains match (can be enhanced with regex)
        return textLower.contains(patternLower);
    }
}

