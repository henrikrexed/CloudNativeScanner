package com.cncf.scanner.service;

import com.cncf.scanner.model.Category;
import com.cncf.scanner.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category("Cloud Native", "Cloud native technologies and practices");
        testCategory.setId(1L);
        testCategory.setEnabled(true);
        testCategory.setKeywordsList(List.of("kubernetes", "docker", "cloud-native"));
        testCategory.setNegativeKeywordsList(List.of("war", "terrorism"));
        testCategory.setSourcesList(List.of("reddit", "stackoverflow", "medium"));
    }

    @Test
    void findAll_returnsAllCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(testCategory));

        List<Category> result = categoryService.findAll();

        assertEquals(1, result.size());
        assertEquals("Cloud Native", result.get(0).getName());
    }

    @Test
    void findEnabled_returnsOnlyEnabled() {
        when(categoryRepository.findEnabled()).thenReturn(List.of(testCategory));

        List<Category> result = categoryService.findEnabled();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getEnabled());
    }

    @Test
    void findById_existingId_returnsCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        Optional<Category> result = categoryService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("Cloud Native", result.get().getName());
    }

    @Test
    void findById_nonExistentId_returnsEmpty() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Category> result = categoryService.findById(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByName_existingName_returnsCategory() {
        when(categoryRepository.findByName("Cloud Native")).thenReturn(Optional.of(testCategory));

        Optional<Category> result = categoryService.findByName("Cloud Native");

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void findByName_nonExistent_returnsEmpty() {
        when(categoryRepository.findByName("NonExistent")).thenReturn(Optional.empty());

        Optional<Category> result = categoryService.findByName("NonExistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void save_delegatesToRepository() {
        when(categoryRepository.save(testCategory)).thenReturn(testCategory);

        Category result = categoryService.save(testCategory);

        assertEquals("Cloud Native", result.getName());
        verify(categoryRepository).save(testCategory);
    }

    @Test
    void create_newCategory_success() {
        when(categoryRepository.existsByName("DevOps")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        Category result = categoryService.create("DevOps", "DevOps practices");

        assertEquals("DevOps", result.getName());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_duplicateName_throwsException() {
        when(categoryRepository.existsByName("Cloud Native")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> categoryService.create("Cloud Native", "duplicate"));
    }

    @Test
    void create_nullName_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> categoryService.create(null, "description"));
    }

    @Test
    void create_blankName_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> categoryService.create("  ", "description"));
    }

    @Test
    void existsByName_delegates() {
        when(categoryRepository.existsByName("Cloud Native")).thenReturn(true);
        when(categoryRepository.existsByName("Unknown")).thenReturn(false);

        assertTrue(categoryService.existsByName("Cloud Native"));
        assertFalse(categoryService.existsByName("Unknown"));
    }

    @Test
    void categoryKeywordsRoundTrip() {
        List<String> keywords = List.of("kubernetes", "docker", "helm");
        testCategory.setKeywordsList(keywords);

        List<String> result = testCategory.getKeywordsList();

        assertEquals(3, result.size());
        assertTrue(result.contains("kubernetes"));
        assertTrue(result.contains("docker"));
        assertTrue(result.contains("helm"));
    }

    @Test
    void categoryNegativeKeywordsRoundTrip() {
        List<String> negKeywords = List.of("war", "military");
        testCategory.setNegativeKeywordsList(negKeywords);

        List<String> result = testCategory.getNegativeKeywordsList();

        assertEquals(2, result.size());
        assertTrue(result.contains("war"));
    }

    @Test
    void categorySourcesRoundTrip() {
        List<String> sources = List.of("reddit", "youtube");
        testCategory.setSourcesList(sources);

        List<String> result = testCategory.getSourcesList();

        assertEquals(2, result.size());
        assertTrue(result.contains("reddit"));
    }

    @Test
    void categoryEmptyKeywords_returnsEmptyList() {
        testCategory.setKeywordsList(List.of());

        List<String> result = testCategory.getKeywordsList();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void categoryNullKeywordsField_returnsEmptyList() {
        Category fresh = new Category("Test", "desc");

        List<String> result = fresh.getKeywordsList();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void delete_callsRepository() {
        categoryService.delete(1L);
        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void enabledField_defaultsToTrue() {
        Category fresh = new Category("Test", "desc");
        assertTrue(fresh.getEnabled());
    }

    @Test
    void enabledField_nullSafety() {
        Category fresh = new Category("Test", "desc");
        fresh.setEnabled(null);
        assertTrue(fresh.getEnabled());
    }
}
