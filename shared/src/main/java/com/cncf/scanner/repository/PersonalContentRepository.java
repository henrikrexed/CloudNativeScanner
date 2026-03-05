package com.cncf.scanner.repository;

import com.cncf.scanner.model.PersonalContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalContentRepository extends JpaRepository<PersonalContent, Long> {
    
    List<PersonalContent> findByUserIdOrderByCreatedAtDesc(String userId);
    
    List<PersonalContent> findByContentTypeOrderByCreatedAtDesc(String contentType);
    
    List<PersonalContent> findByCategoryOrderByCreatedAtDesc(String category);
    
    @Query("SELECT pc FROM PersonalContent pc WHERE pc.ragStored = false ORDER BY pc.createdAt ASC")
    List<PersonalContent> findUnprocessedContent();
    
    @Query("SELECT pc FROM PersonalContent pc WHERE pc.ragStored = true ORDER BY pc.createdAt DESC")
    List<PersonalContent> findProcessedContent();
    
    @Query("SELECT pc FROM PersonalContent pc WHERE pc.tags LIKE %:tag% ORDER BY pc.createdAt DESC")
    List<PersonalContent> findByTagContaining(@Param("tag") String tag);
    
    @Query("SELECT pc FROM PersonalContent pc WHERE pc.category = :category AND pc.contentType = :contentType ORDER BY pc.createdAt DESC")
    List<PersonalContent> findByCategoryAndContentType(@Param("category") String category, @Param("contentType") String contentType);
    
    List<PersonalContent> findAllByOrderByCreatedAtDesc();
    
    Optional<PersonalContent> findById(Long id);
}
