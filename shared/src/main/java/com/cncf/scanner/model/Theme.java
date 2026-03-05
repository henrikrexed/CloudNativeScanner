package com.cncf.scanner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.Hibernate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "themes")
public class Theme {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "search_keywords", columnDefinition = "TEXT")
    private String searchKeywords; // JSON array of keywords, e.g., ["kubernetes", "k8s", "container orchestration", ...]
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true; // Whether this theme is enabled for topic collection
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_theme_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "parentTheme", "topicThemes"})
    private Theme parentTheme;
    
    @OneToMany(mappedBy = "parentTheme", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "parentTheme", "topicThemes"})
    private Set<Theme> subThemes;
    
    @OneToMany(mappedBy = "theme", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "theme", "topic"})
    private Set<TopicTheme> topicThemes;
    
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
    public Theme() {}
    
    public Theme(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getSearchKeywords() {
        return searchKeywords;
    }
    
    public void setSearchKeywords(String searchKeywords) {
        this.searchKeywords = searchKeywords;
    }
    
    /**
     * Get search keywords as a list
     * Returns empty list if keywords are null or empty
     */
    @JsonProperty("keywords")
    public List<String> getKeywordsList() {
        if (searchKeywords == null || searchKeywords.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        try {
            // Try to parse as JSON array
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(searchKeywords, java.util.List.class);
        } catch (Exception e) {
            // Fallback: treat as comma-separated string
            return java.util.Arrays.asList(searchKeywords.split(",\\s*"));
        }
    }
    
    /**
     * Set search keywords from a list
     */
    public void setKeywordsList(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            this.searchKeywords = null;
        } else {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                this.searchKeywords = mapper.writeValueAsString(keywords);
            } catch (Exception e) {
                // Fallback: use comma-separated string
                this.searchKeywords = String.join(", ", keywords);
            }
        }
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
    
    public Set<TopicTheme> getTopicThemes() {
        return topicThemes;
    }
    
    public void setTopicThemes(Set<TopicTheme> topicThemes) {
        this.topicThemes = topicThemes;
    }
    
    public Theme getParentTheme() {
        return parentTheme;
    }
    
    public void setParentTheme(Theme parentTheme) {
        this.parentTheme = parentTheme;
    }
    
    public Set<Theme> getSubThemes() {
        return subThemes;
    }
    
    public void setSubThemes(Set<Theme> subThemes) {
        this.subThemes = subThemes;
    }
    
    @JsonProperty("enabled")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public Boolean getEnabled() {
        // Return the actual value - don't default here to ensure false values are preserved
        // The defaulting to true should only happen in setEnabled when explicitly null
        // Since the database column is NOT NULL, this should never be null
        return enabled != null ? enabled : true;
    }
    
    @JsonProperty("enabled")
    public void setEnabled(Boolean enabled) {
        // Accept explicit true/false from API; default to true only when value is null
        // IMPORTANT: If enabled is explicitly false, preserve it (don't default to true)
        this.enabled = enabled;
        // Only default to true if explicitly null (not provided in request)
        if (this.enabled == null) {
            this.enabled = true;
        }
    }
    
    /**
     * Get the full theme path (e.g., "Kubernetes/Networking")
     * This is a computed property that will be included in JSON responses
     * Includes protection against circular references and infinite recursion
     */
    @JsonProperty("fullPath")
    public String getFullPath() {
        return getFullPath(new java.util.HashSet<>());
    }
    
    /**
     * Internal method to get full path with cycle detection
     */
    private String getFullPath(java.util.Set<Long> visited) {
        try {
            if (name == null) {
                return "";
            }
            
            // Prevent infinite recursion in case of circular parent-child relationships
            if (visited.contains(this.id)) {
                return name; // Circular reference detected, return just the name
            }
            visited.add(this.id);
            
            if (parentTheme != null) {
                // Check if parent theme is initialized to avoid LazyInitializationException
                try {
                    if (Hibernate.isInitialized(parentTheme)) {
                        // Safely get parent path - handle potential recursion issues
                        String parentPath = null;
                        try {
                            // Use the internal method with visited set to prevent cycles
                            parentPath = parentTheme.getFullPath(new java.util.HashSet<>(visited));
                        } catch (org.hibernate.LazyInitializationException e) {
                            // Parent's parent not loaded, just use parent name
                            parentPath = parentTheme.getName() != null ? parentTheme.getName() : "";
                        } catch (Exception e) {
                            // Any other error, use parent name
                            parentPath = parentTheme.getName() != null ? parentTheme.getName() : "";
                        }
                        if (parentPath != null && !parentPath.isEmpty() && !parentPath.equals(name)) {
                            return parentPath + "/" + name;
                        } else {
                            return name;
                        }
                    } else {
                        // Parent theme not initialized, just return the theme name
                        return name;
                    }
                } catch (org.hibernate.LazyInitializationException e) {
                    // If parent theme is not loaded, just return the theme name
                    return name;
                }
            }
            return name;
        } catch (Exception e) {
            // If any error occurs, just return the theme name or empty string
            return name != null ? name : "";
        }
    }
    
    /**
     * Check if this theme is a top-level theme (no parent)
     */
    public boolean isTopLevel() {
        return parentTheme == null;
    }
}



