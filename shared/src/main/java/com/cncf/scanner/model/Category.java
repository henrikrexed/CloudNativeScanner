package com.cncf.scanner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "categories")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Category {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    @Column(name = "negative_keywords", columnDefinition = "TEXT")
    private String negativeKeywords;

    @Column(name = "sources", columnDefinition = "TEXT")
    private String sources;

    @Column(name = "scan_frequency", length = 20)
    private String scanFrequency = "daily";

    @Column(nullable = false)
    private Boolean enabled = true;

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

    public Category() {}

    public Category(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getScanFrequency() { return scanFrequency; }
    public void setScanFrequency(String scanFrequency) { this.scanFrequency = scanFrequency; }

    public Boolean getEnabled() { return enabled != null ? enabled : true; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled != null ? enabled : true; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Keywords as List helpers (stored as JSON array string)
    public List<String> getKeywordsList() {
        return parseJsonArray(keywords);
    }

    public void setKeywordsList(List<String> list) {
        this.keywords = toJsonArray(list);
    }

    public List<String> getNegativeKeywordsList() {
        return parseJsonArray(negativeKeywords);
    }

    public void setNegativeKeywordsList(List<String> list) {
        this.negativeKeywords = toJsonArray(list);
    }

    public List<String> getSourcesList() {
        return parseJsonArray(sources);
    }

    public void setSourcesList(List<String> list) {
        this.sources = toJsonArray(list);
    }

    // Raw string accessors for JPA
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getNegativeKeywords() { return negativeKeywords; }
    public void setNegativeKeywords(String negativeKeywords) { this.negativeKeywords = negativeKeywords; }

    public String getSources() { return sources; }
    public void setSources(String sources) { this.sources = sources; }

    private static List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return new ArrayList<>(Arrays.asList(json.split(",\\s*")));
        }
    }

    private static String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            return String.join(", ", list);
        }
    }
}
