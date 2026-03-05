package com.cncf.scanner.service;

import com.cncf.scanner.model.Theme;
import com.cncf.scanner.repository.ThemeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ThemeService {
    
    private static final Logger logger = LoggerFactory.getLogger(ThemeService.class);
    
    private final ThemeRepository themeRepository;
    
    @Autowired
    public ThemeService(ThemeRepository themeRepository) {
        this.themeRepository = themeRepository;
    }
    
    /**
     * Find all themes (including disabled ones)
     */
    public List<Theme> findAll() {
        return themeRepository.findAll();
    }
    
    /**
     * Find all enabled themes (for scanning purposes)
     */
    public List<Theme> findEnabledThemes() {
        return themeRepository.findEnabledThemes();
    }
    
    /**
     * Find theme by ID
     */
    public Optional<Theme> findById(Long id) {
        return themeRepository.findById(id);
    }
    
    /**
     * Find theme by name
     */
    public Theme findByName(String name) {
        return themeRepository.findByName(name).orElse(null);
    }
    
    /**
     * Save or update a theme
     * Note: Keyword generation should be handled by the calling service (ThemeKeywordGenerationService)
     * to avoid circular dependencies, as ThemeService is in the shared module.
     */
    public Theme save(Theme theme) {
        return themeRepository.save(theme);
    }
    
    /**
     * Create a new theme
     */
    public Theme createTheme(String name, String description) {
        if (themeRepository.existsByName(name)) {
            throw new IllegalArgumentException("Theme with name '" + name + "' already exists");
        }
        
        Theme theme = new Theme(name, description);
        return themeRepository.save(theme);
    }
    
    /**
     * Update theme
     */
    public Theme updateTheme(Long id, String name, String description) {
        Theme theme = themeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Theme not found with id: " + id));
        
        theme.setName(name);
        theme.setDescription(description);
        
        return themeRepository.save(theme);
    }
    
    /**
     * Delete theme
     */
    public void delete(Long id) {
        themeRepository.deleteById(id);
    }
    
    /**
     * Check if theme exists
     */
    public boolean existsByName(String name) {
        return themeRepository.existsByName(name);
    }
    
    /**
     * Find themes by parent theme
     */
    public List<Theme> findByParentThemeId(Long parentId) {
        return themeRepository.findByParentThemeId(parentId);
    }
    
    /**
     * Find enabled themes by parent theme
     */
    public List<Theme> findEnabledByParentThemeId(Long parentId) {
        return themeRepository.findEnabledByParentThemeId(parentId);
    }
    
    /**
     * Find top-level themes (no parent)
     */
    public List<Theme> findTopLevelThemes() {
        return themeRepository.findTopLevelThemes();
    }
    
    /**
     * Find enabled top-level themes (no parent)
     */
    public List<Theme> findEnabledTopLevelThemes() {
        return themeRepository.findEnabledTopLevelThemes();
    }
    
    /**
     * Find or create theme by full path (e.g., "Kubernetes/Networking")
     * Uses RAG to check existing themes before creating new ones
     */
    public Theme findOrCreateThemeByFullPath(String fullPath, String description) {
        if (fullPath == null || fullPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Theme path cannot be empty");
        }
        
        String[] parts = fullPath.split("/");
        if (parts.length == 0) {
            throw new IllegalArgumentException("Invalid theme path: " + fullPath);
        }
        
        // Find or create parent theme
        Theme parentTheme = null;
        if (parts.length > 1) {
            String parentName = parts[0].trim();
            parentTheme = findOrCreateTheme(parentName, null);
            
            // Find or create sub-theme
            String subThemeName = parts[1].trim();
            return findOrCreateSubTheme(parentTheme, subThemeName, description);
        } else {
            // Top-level theme
            return findOrCreateTheme(parts[0].trim(), description);
        }
    }
    
    /**
     * Find or create a theme (top-level)
     */
    private Theme findOrCreateTheme(String name, String description) {
        Theme existing = findByName(name);
        if (existing != null) {
            return existing;
        }
        
        // Create new theme
        Theme theme = new Theme(name, description);
        return themeRepository.save(theme);
    }
    
    /**
     * Find or create a sub-theme under a parent
     */
    private Theme findOrCreateSubTheme(Theme parentTheme, String name, String description) {
        // Check if sub-theme already exists
        List<Theme> subThemes = findByParentThemeId(parentTheme.getId());
        for (Theme subTheme : subThemes) {
            if (subTheme.getName().equalsIgnoreCase(name)) {
                return subTheme;
            }
        }
        
        // Create new sub-theme
        Theme subTheme = new Theme(name, description);
        subTheme.setParentTheme(parentTheme);
        return themeRepository.save(subTheme);
    }
    
    /**
     * Find similar themes using RAG (semantic similarity)
     * This helps limit sub-category creation by suggesting existing themes
     */
    public List<Theme> findSimilarThemes(String themeName, double similarityThreshold) {
        // Simple implementation: find themes with similar names
        // In production, this would use vector similarity search
        List<Theme> allThemes = findAll();
        List<Theme> similar = new ArrayList<>();
        
        String lowerName = themeName.toLowerCase();
        for (Theme theme : allThemes) {
            String themeLower = theme.getName().toLowerCase();
            // Check if names are similar (contains or is contained)
            if (themeLower.contains(lowerName) || lowerName.contains(themeLower)) {
                similar.add(theme);
            }
        }
        
        return similar;
    }
}
