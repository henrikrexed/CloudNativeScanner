package com.cncf.scanner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "personal_content")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PersonalContent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType = "GENERAL"; // GENERAL, BLOG, VIDEO_SCRIPT, ARTICLE, etc.
    
    @Column(name = "category", length = 100)
    private String category; // Main category for broader organization
    
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags; // JSON array of tags, e.g., ["kubernetes", "beginner", "tutorial"]
    
    @Column(name = "user_id", length = 255)
    private String userId; // For future multi-user support
    
    @Column(name = "writing_style_metadata", columnDefinition = "TEXT")
    private String writingStyleMetadata; // JSON string containing style analysis
    
    @Column(name = "rag_stored")
    private Boolean ragStored = false;
    
    @Column(name = "rag_stored_at")
    private LocalDateTime ragStoredAt;
    
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
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
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
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getWritingStyleMetadata() {
        return writingStyleMetadata;
    }
    
    public void setWritingStyleMetadata(String writingStyleMetadata) {
        this.writingStyleMetadata = writingStyleMetadata;
    }
    
    public Boolean getRagStored() {
        return ragStored;
    }
    
    public void setRagStored(Boolean ragStored) {
        this.ragStored = ragStored;
    }
    
    public LocalDateTime getRagStoredAt() {
        return ragStoredAt;
    }
    
    public void setRagStoredAt(LocalDateTime ragStoredAt) {
        this.ragStoredAt = ragStoredAt;
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
