package com.cncf.scanner.controller;

import com.cncf.scanner.model.Topic;
import com.cncf.scanner.model.Theme;
import com.cncf.scanner.service.TopicService;
import com.cncf.scanner.service.ThemeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class TopicController {
    
    private final TopicService topicService;
    private final ThemeService themeService;
    
    @Autowired
    public TopicController(TopicService topicService, ThemeService themeService) {
        this.topicService = topicService;
        this.themeService = themeService;
    }
    
    @GetMapping("/")
    public String index(Model model) {
        List<Theme> themes = themeService.findAll();
        model.addAttribute("themes", themes);
        return "index";
    }
    
    @GetMapping("/themes")
    public String themes(Model model) {
        List<Theme> themes = themeService.findAll();
        model.addAttribute("themes", themes);
        return "themes";
    }
    
    @GetMapping("/themes/{themeId}")
    public String topicsByTheme(@PathVariable Long themeId, 
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               Model model) {
        Theme theme = themeService.findById(themeId)
                .orElseThrow(() -> new IllegalArgumentException("Theme not found"));
        
        List<Topic> topics = topicService.findByThemeId(themeId);
        
        model.addAttribute("theme", theme);
        model.addAttribute("topics", topics);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        
        return "topics-by-theme";
    }
    
    @GetMapping("/topics/recent")
    public String recentTopics(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              Model model) {
        List<Topic> topics = topicService.findRecentTopics();
        
        model.addAttribute("topics", topics);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("title", "Recent Topics");
        
        return "topics-list";
    }
    
    @GetMapping("/topics/popular")
    public String popularTopics(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               Model model) {
        List<Topic> topics = topicService.findPopularTopics(10); // Min 10 interactions
        
        model.addAttribute("topics", topics);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("title", "Popular Topics");
        
        return "topics-list";
    }
    
    @GetMapping("/topics/{topicId}")
    public String topicDetails(@PathVariable Long topicId, Model model) {
        Topic topic = topicService.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found"));
        
        model.addAttribute("topic", topic);
        return "topic-details";
    }
}


