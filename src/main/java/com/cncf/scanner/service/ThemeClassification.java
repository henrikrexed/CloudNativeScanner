package com.cncf.scanner.service;

import com.cncf.scanner.model.Theme;

public class ThemeClassification {
    
    private Theme theme;
    private Double confidenceScore;
    
    public ThemeClassification() {}
    
    public ThemeClassification(Theme theme, Double confidenceScore) {
        this.theme = theme;
        this.confidenceScore = confidenceScore;
    }
    
    public Theme getTheme() {
        return theme;
    }
    
    public void setTheme(Theme theme) {
        this.theme = theme;
    }
    
    public Double getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
}


