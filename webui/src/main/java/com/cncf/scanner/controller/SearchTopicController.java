package com.cncf.scanner.controller;

import com.cncf.scanner.model.SearchTopic;
import com.cncf.scanner.model.Source;
import com.cncf.scanner.service.SearchTopicService;
import com.cncf.scanner.service.SourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/search-topics")
public class SearchTopicController {
    
    private final SearchTopicService searchTopicService;
    private final SourceService sourceService;
    
    @Autowired
    public SearchTopicController(SearchTopicService searchTopicService, SourceService sourceService) {
        this.searchTopicService = searchTopicService;
        this.sourceService = sourceService;
    }
    
    @GetMapping
    public String searchTopics(@RequestParam(required = false) Long sourceId, Model model) {
        List<SearchTopic> searchTopics;
        if (sourceId != null) {
            searchTopics = searchTopicService.findBySourceId(sourceId);
            model.addAttribute("selectedSourceId", sourceId);
        } else {
            searchTopics = searchTopicService.findAll();
        }
        
        List<Source> sources = sourceService.findAll();
        
        model.addAttribute("searchTopics", searchTopics);
        model.addAttribute("sources", sources);
        
        return "admin/search-topics";
    }
    
    @GetMapping("/new")
    public String newSearchTopicForm(@RequestParam(required = false) Long sourceId, Model model) {
        SearchTopic searchTopic = new SearchTopic();
        if (sourceId != null) {
            Source source = sourceService.findById(sourceId)
                    .orElseThrow(() -> new IllegalArgumentException("Source not found"));
            searchTopic.setSource(source);
        }
        
        List<Source> sources = sourceService.findAll();
        
        model.addAttribute("searchTopic", searchTopic);
        model.addAttribute("sources", sources);
        
        return "admin/search-topic-form";
    }
    
    @GetMapping("/{id}/edit")
    public String editSearchTopicForm(@PathVariable Long id, Model model) {
        SearchTopic searchTopic = searchTopicService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Search topic not found"));
        
        List<Source> sources = sourceService.findAll();
        
        model.addAttribute("searchTopic", searchTopic);
        model.addAttribute("sources", sources);
        
        return "admin/search-topic-form";
    }
    
    @PostMapping
    public String saveSearchTopic(@ModelAttribute SearchTopic searchTopic, RedirectAttributes redirectAttributes) {
        try {
            if (searchTopic.getId() == null) {
                searchTopicService.createSearchTopic(
                        searchTopic.getSource(),
                        searchTopic.getKeyword(),
                        searchTopic.getSearchQuery(),
                        searchTopic.getDescription(),
                        searchTopic.getPriority(),
                        searchTopic.getMaxResults()
                );
                redirectAttributes.addFlashAttribute("successMessage", "Search topic created successfully");
            } else {
                searchTopicService.updateSearchTopic(
                        searchTopic.getId(),
                        searchTopic.getKeyword(),
                        searchTopic.getSearchQuery(),
                        searchTopic.getDescription(),
                        searchTopic.getIsActive(),
                        searchTopic.getPriority(),
                        searchTopic.getMaxResults(),
                        searchTopic.getSearchFrequencyHours()
                );
                redirectAttributes.addFlashAttribute("successMessage", "Search topic updated successfully");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving search topic: " + e.getMessage());
        }
        return "redirect:/admin/search-topics";
    }
    
    @PostMapping("/{id}/toggle")
    public String toggleSearchTopic(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            searchTopicService.toggleSearchTopicStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Search topic status toggled successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error toggling search topic: " + e.getMessage());
        }
        return "redirect:/admin/search-topics";
    }
    
    @PostMapping("/{id}/delete")
    public String deleteSearchTopic(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            searchTopicService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Search topic deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting search topic: " + e.getMessage());
        }
        return "redirect:/admin/search-topics";
    }
    
    @GetMapping("/source/{sourceId}")
    public String searchTopicsBySource(@PathVariable Long sourceId, Model model) {
        List<SearchTopic> searchTopics = searchTopicService.findBySourceId(sourceId);
        Source source = sourceService.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source not found"));
        
        model.addAttribute("searchTopics", searchTopics);
        model.addAttribute("source", source);
        
        return "admin/search-topics-by-source";
    }
}


