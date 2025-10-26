package com.cncf.scanner.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "sources")
public class Source {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    
    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;
    
    @Column(name = "api_endpoint", length = 500)
    private String apiEndpoint;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "scan_frequency_hours")
    private Integer scanFrequencyHours = 24;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Topic> topics;
    
    @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ScanHistory> scanHistories;
    
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
    public Source() {}
    
    public Source(String name, String baseUrl, String apiEndpoint) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.apiEndpoint = apiEndpoint;
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
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getApiEndpoint() {
        return apiEndpoint;
    }
    
    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Integer getScanFrequencyHours() {
        return scanFrequencyHours;
    }
    
    public void setScanFrequencyHours(Integer scanFrequencyHours) {
        this.scanFrequencyHours = scanFrequencyHours;
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
    
    public Set<Topic> getTopics() {
        return topics;
    }
    
    public void setTopics(Set<Topic> topics) {
        this.topics = topics;
    }
    
    public Set<ScanHistory> getScanHistories() {
        return scanHistories;
    }
    
    public void setScanHistories(Set<ScanHistory> scanHistories) {
        this.scanHistories = scanHistories;
    }
}
