package com.cncf.scanner.service;

import com.cncf.scanner.model.Theme;
import com.cncf.scanner.repository.ThemeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Find all themes
     */
    public List<Theme> findAll() {
        return themeRepository.findAll();
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
}


