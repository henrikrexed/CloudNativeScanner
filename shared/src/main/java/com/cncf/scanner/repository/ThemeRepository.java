package com.cncf.scanner.repository;

import com.cncf.scanner.model.Theme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    
    /**
     * Find themes by parent theme
     */
    @Query("SELECT t FROM Theme t WHERE t.parentTheme.id = :parentId")
    List<Theme> findByParentThemeId(@Param("parentId") Long parentId);
    
    /**
     * Find top-level themes (no parent)
     */
    @Query("SELECT t FROM Theme t WHERE t.parentTheme IS NULL")
    List<Theme> findTopLevelThemes();
    
    /**
     * Find theme by full path (e.g., "Kubernetes/Networking")
     */
    @Query("SELECT t FROM Theme t WHERE t.name = :name AND " +
           "(:parentName IS NULL OR t.parentTheme.name = :parentName)")
    Optional<Theme> findByFullPath(@Param("name") String name, @Param("parentName") String parentName);
    
    /**
     * Find all enabled themes
     * Note: enabled column is NOT NULL DEFAULT true, so we only need to check for true
     */
    @Query("SELECT t FROM Theme t WHERE t.enabled = true")
    List<Theme> findEnabledThemes();
    
    /**
     * Find top-level enabled themes (no parent)
     * Note: enabled column is NOT NULL DEFAULT true, so we only need to check for true
     */
    @Query("SELECT t FROM Theme t WHERE t.parentTheme IS NULL AND t.enabled = true")
    List<Theme> findEnabledTopLevelThemes();
    
    /**
     * Find enabled themes by parent theme
     * Note: enabled column is NOT NULL DEFAULT true, so we only need to check for true
     */
    @Query("SELECT t FROM Theme t WHERE t.parentTheme.id = :parentId AND t.enabled = true")
    List<Theme> findEnabledByParentThemeId(@Param("parentId") Long parentId);
}
