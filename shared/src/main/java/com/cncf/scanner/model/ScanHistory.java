package com.cncf.scanner.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scan_history")
public class ScanHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;
    
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "topics_found")
    private Integer topicsFound = 0;
    
    @Column(name = "topics_processed")
    private Integer topicsProcessed = 0;
    
    @Column(name = "topics_new")
    private Integer topicsNew = 0;
    
    @Column(length = 50)
    private String status = "RUNNING";
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Constructors
    public ScanHistory() {}
    
    public ScanHistory(Source source, LocalDateTime startedAt) {
        this.source = source;
        this.startedAt = startedAt;
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
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public Integer getTopicsFound() {
        return topicsFound;
    }
    
    public void setTopicsFound(Integer topicsFound) {
        this.topicsFound = topicsFound;
    }
    
    public Integer getTopicsProcessed() {
        return topicsProcessed;
    }
    
    public void setTopicsProcessed(Integer topicsProcessed) {
        this.topicsProcessed = topicsProcessed;
    }
    
    public Integer getTopicsNew() {
        return topicsNew;
    }
    
    public void setTopicsNew(Integer topicsNew) {
        this.topicsNew = topicsNew;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
