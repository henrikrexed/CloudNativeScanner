package com.cncf.scanner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "topics", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"source_id", "external_id"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Topic {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Source source;
    
    @Column(name = "external_id", nullable = false)
    private String externalId;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(nullable = false, length = 1000)
    private String url;
    
    @Column(name = "interaction_count")
    private Integer interactionCount = 0;
    
    @Column(name = "view_count")
    private Integer viewCount = 0;
    
    @Column
    private Integer score = 0;
    
    @Column
    private String author;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "collected_at")
    private LocalDateTime collectedAt; // Date when topic was first collected by the scanner
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_scanned_at")
    private LocalDateTime lastScannedAt;
    
    @Column(name = "engagement_score", precision = 5, scale = 2)
    private java.math.BigDecimal engagementScore = java.math.BigDecimal.ZERO;
    
    @Column(name = "is_hot_topic")
    private Boolean isHotTopic = false;
    
    @Column(name = "conversation_summary", columnDefinition = "TEXT")
    private String conversationSummary;
    
    @Column(name = "conversation_collected_at")
    private LocalDateTime conversationCollectedAt;
    
    @Column(name = "technical_quality_score", precision = 5, scale = 2)
    private java.math.BigDecimal technicalQualityScore = java.math.BigDecimal.ZERO;
    
    @Column(name = "marketing_score", precision = 5, scale = 2)
    private java.math.BigDecimal marketingScore = java.math.BigDecimal.ZERO;
    
    @Column(name = "conversation_rag_stored")
    private Boolean conversationRagStored = false;
    
    @Column(name = "topic_group_id")
    private Long topicGroupId;
    
    @Column(name = "thumbs_up")
    private Integer thumbsUp = 0;
    
    @Column(name = "thumbs_down")
    private Integer thumbsDown = 0;
    
    @Column(name = "is_rejected")
    private Boolean isRejected = false;
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    @Column(name = "feedback_learned")
    private Boolean feedbackLearned = false; // Whether feedback has been used to improve scanner
    
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags; // JSON array of tags, e.g., ["tutorial", "beginner", "kubernetes", "networking"]
    
    @Column(name = "content_extraction_status", length = 50)
    private String contentExtractionStatus = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED
    
    @Column(name = "content_extraction_attempted_at")
    private LocalDateTime contentExtractionAttemptedAt;
    
    @Column(name = "content_extraction_completed_at")
    private LocalDateTime contentExtractionCompletedAt;
    
    @Column(name = "content_summary", columnDefinition = "TEXT")
    private String contentSummary; // LLM-generated summary for RAG
    
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "topic", "theme"})
    private Set<TopicTheme> topicThemes = new java.util.HashSet<>();
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        // Set collectedAt only if not already set (allows manual setting before save)
        if (collectedAt == null) {
            collectedAt = now;
        }
        updatedAt = now;
        lastScannedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        lastScannedAt = LocalDateTime.now();
    }
    
    // Constructors
    public Topic() {}
    
    public Topic(Source source, String externalId, String title, String url) {
        this.source = source;
        this.externalId = externalId;
        this.title = title;
        this.url = url;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Source getSource() {
        return source;
    }
    
    public void setSource(Source source) {
        this.source = source;
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
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }
    
    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getLastScannedAt() {
        return lastScannedAt;
    }
    
    public void setLastScannedAt(LocalDateTime lastScannedAt) {
        this.lastScannedAt = lastScannedAt;
    }
    
    public Set<TopicTheme> getTopicThemes() {
        return topicThemes;
    }
    
    public void setTopicThemes(Set<TopicTheme> topicThemes) {
        this.topicThemes = topicThemes;
    }
    
    public java.math.BigDecimal getEngagementScore() {
        return engagementScore;
    }
    
    public void setEngagementScore(java.math.BigDecimal engagementScore) {
        this.engagementScore = engagementScore;
    }
    
    public Boolean getIsHotTopic() {
        return isHotTopic != null ? isHotTopic : false;
    }
    
    public void setIsHotTopic(Boolean isHotTopic) {
        this.isHotTopic = isHotTopic;
    }
    
    public String getConversationSummary() {
        return conversationSummary;
    }
    
    public void setConversationSummary(String conversationSummary) {
        this.conversationSummary = conversationSummary;
    }
    
    public LocalDateTime getConversationCollectedAt() {
        return conversationCollectedAt;
    }
    
    public void setConversationCollectedAt(LocalDateTime conversationCollectedAt) {
        this.conversationCollectedAt = conversationCollectedAt;
    }
    
    public java.math.BigDecimal getTechnicalQualityScore() {
        return technicalQualityScore;
    }
    
    public void setTechnicalQualityScore(java.math.BigDecimal technicalQualityScore) {
        this.technicalQualityScore = technicalQualityScore;
    }
    
    public java.math.BigDecimal getMarketingScore() {
        return marketingScore;
    }
    
    public void setMarketingScore(java.math.BigDecimal marketingScore) {
        this.marketingScore = marketingScore;
    }
    
    public Boolean getConversationRagStored() {
        return conversationRagStored != null ? conversationRagStored : false;
    }
    
    public void setConversationRagStored(Boolean conversationRagStored) {
        this.conversationRagStored = conversationRagStored;
    }
    
    public Long getTopicGroupId() {
        return topicGroupId;
    }
    
    public void setTopicGroupId(Long topicGroupId) {
        this.topicGroupId = topicGroupId;
    }
    
    public Integer getThumbsUp() {
        return thumbsUp != null ? thumbsUp : 0;
    }
    
    public void setThumbsUp(Integer thumbsUp) {
        this.thumbsUp = thumbsUp;
    }
    
    public Integer getThumbsDown() {
        return thumbsDown != null ? thumbsDown : 0;
    }
    
    public void setThumbsDown(Integer thumbsDown) {
        this.thumbsDown = thumbsDown;
    }
    
    public Boolean getIsRejected() {
        return isRejected != null ? isRejected : false;
    }
    
    public void setIsRejected(Boolean isRejected) {
        this.isRejected = isRejected;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    public Boolean getFeedbackLearned() {
        return feedbackLearned != null ? feedbackLearned : false;
    }
    
    public void setFeedbackLearned(Boolean feedbackLearned) {
        this.feedbackLearned = feedbackLearned;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
    
    /**
     * Get tags as a list for convenient access
     */
    @JsonProperty("tagsList")
    public List<String> getTagsList() {
        if (tags == null || tags.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(tags, java.util.List.class);
        } catch (Exception e) {
            // Fallback: treat as comma-separated
            return java.util.Arrays.asList(tags.split(",\\s*"));
        }
    }
    
    /**
     * Set tags from a list
     */
    public void setTagsList(List<String> tagsList) {
        if (tagsList == null || tagsList.isEmpty()) {
            this.tags = null;
        } else {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                this.tags = mapper.writeValueAsString(tagsList);
            } catch (Exception e) {
                // Fallback: join with commas
                this.tags = String.join(", ", tagsList);
            }
        }
    }
    
    // Content extraction status getters and setters
    public String getContentExtractionStatus() {
        return contentExtractionStatus;
    }
    
    public void setContentExtractionStatus(String contentExtractionStatus) {
        this.contentExtractionStatus = contentExtractionStatus;
    }
    
    public LocalDateTime getContentExtractionAttemptedAt() {
        return contentExtractionAttemptedAt;
    }
    
    public void setContentExtractionAttemptedAt(LocalDateTime contentExtractionAttemptedAt) {
        this.contentExtractionAttemptedAt = contentExtractionAttemptedAt;
    }
    
    public LocalDateTime getContentExtractionCompletedAt() {
        return contentExtractionCompletedAt;
    }
    
    public void setContentExtractionCompletedAt(LocalDateTime contentExtractionCompletedAt) {
        this.contentExtractionCompletedAt = contentExtractionCompletedAt;
    }
    
    public String getContentSummary() {
        return contentSummary;
    }
    
    public void setContentSummary(String contentSummary) {
        this.contentSummary = contentSummary;
    }
    
    // Helper methods for content extraction status
    public boolean isContentExtractionPending() {
        return "PENDING".equals(contentExtractionStatus);
    }
    
    public boolean isContentExtractionCompleted() {
        return "COMPLETED".equals(contentExtractionStatus);
    }
    
    public boolean isContentExtractionProcessing() {
        return "PROCESSING".equals(contentExtractionStatus);
    }
    
    public boolean isContentExtractionFailed() {
        return "FAILED".equals(contentExtractionStatus);
    }
}
