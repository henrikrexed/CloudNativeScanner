package com.cncf.scanner.repository;

import com.cncf.scanner.model.Theme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ThemeRepository extends JpaRepository<Theme, Long> {
    
    /**
     * Find theme by name
     */
    Optional<Theme> findByName(String name);
    
    /**
     * Check if theme exists by name
     */
    boolean existsByName(String name);
}


