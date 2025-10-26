package com.cncf.scanner.controller;

import com.cncf.scanner.model.Source;
import com.cncf.scanner.model.Theme;
import com.cncf.scanner.service.SourceService;
import com.cncf.scanner.service.ThemeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    private final SourceService sourceService;
    private final ThemeService themeService;
    
    @Autowired
    public AdminController(SourceService sourceService, 
                          ThemeService themeService) {
        this.sourceService = sourceService;
        this.themeService = themeService;
    }
    
    @GetMapping
    public String adminDashboard(Model model) {
        List<Source> sources = sourceService.findAll();
        List<Theme> themes = themeService.findAll();
        
        model.addAttribute("sources", sources);
        model.addAttribute("themes", themes);
        model.addAttribute("activeSources", sourceService.findActiveSources().size());
        model.addAttribute("totalSources", sources.size());
        
        return "admin/dashboard";
    }
    
    // Source Management
    @GetMapping("/sources")
    public String sources(Model model) {
        List<Source> sources = sourceService.findAll();
        model.addAttribute("sources", sources);
        return "admin/sources";
    }
    
    @GetMapping("/sources/new")
    public String newSourceForm(Model model) {
        model.addAttribute("source", new Source());
        return "admin/source-form";
    }
    
    @GetMapping("/sources/{id}/edit")
    public String editSourceForm(@PathVariable Long id, Model model) {
        Source source = sourceService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Source not found"));
        model.addAttribute("source", source);
        return "admin/source-form";
    }
    
    @PostMapping("/sources")
    public String saveSource(@ModelAttribute Source source, RedirectAttributes redirectAttributes) {
        try {
            if (source.getId() == null) {
                sourceService.createSource(source.getName(), source.getBaseUrl(), source.getApiEndpoint());
                redirectAttributes.addFlashAttribute("successMessage", "Source created successfully");
            } else {
                sourceService.updateSource(source.getId(), source.getName(), source.getBaseUrl(), 
                        source.getApiEndpoint(), source.getIsActive(), source.getScanFrequencyHours());
                redirectAttributes.addFlashAttribute("successMessage", "Source updated successfully");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving source: " + e.getMessage());
        }
        return "redirect:/admin/sources";
    }
    
    @PostMapping("/sources/{id}/toggle")
    public String toggleSource(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            sourceService.toggleSourceStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Source status toggled successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error toggling source: " + e.getMessage());
        }
        return "redirect:/admin/sources";
    }
    
    @PostMapping("/sources/{id}/delete")
    public String deleteSource(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            sourceService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Source deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting source: " + e.getMessage());
        }
        return "redirect:/admin/sources";
    }
    
    @PostMapping("/sources/{id}/scan")
    public String scanSource(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // In a microservices architecture, this would trigger a scan via API call to topic-scanner service
            // For now, we'll just show a message that scanning is handled by the CronJob
            redirectAttributes.addFlashAttribute("successMessage", "Source scanning is handled automatically by the CronJob");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/admin/sources";
    }
    
    // Theme Management
    @GetMapping("/themes")
    public String themes(Model model) {
        List<Theme> themes = themeService.findAll();
        model.addAttribute("themes", themes);
        return "admin/themes";
    }
    
    @GetMapping("/themes/new")
    public String newThemeForm(Model model) {
        model.addAttribute("theme", new Theme());
        return "admin/theme-form";
    }
    
    @GetMapping("/themes/{id}/edit")
    public String editThemeForm(@PathVariable Long id, Model model) {
        Theme theme = themeService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Theme not found"));
        model.addAttribute("theme", theme);
        return "admin/theme-form";
    }
    
    @PostMapping("/themes")
    public String saveTheme(@ModelAttribute Theme theme, RedirectAttributes redirectAttributes) {
        try {
            if (theme.getId() == null) {
                themeService.createTheme(theme.getName(), theme.getDescription());
                redirectAttributes.addFlashAttribute("successMessage", "Theme created successfully");
            } else {
                themeService.updateTheme(theme.getId(), theme.getName(), theme.getDescription());
                redirectAttributes.addFlashAttribute("successMessage", "Theme updated successfully");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving theme: " + e.getMessage());
        }
        return "redirect:/admin/themes";
    }
    
    @PostMapping("/themes/{id}/delete")
    public String deleteTheme(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            themeService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Theme deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting theme: " + e.getMessage());
        }
        return "redirect:/admin/themes";
    }
    
    // System Configuration
    @GetMapping("/config")
    public String systemConfig(Model model) {
        // This would typically read from ConfigMaps or a configuration service
        model.addAttribute("cronSchedule", "0 2 * * *"); // Default daily at 2 AM
        model.addAttribute("kafkaTopic", "topic-scanner");
        model.addAttribute("maxTopicsPerScan", 100);
        return "admin/config";
    }
    
    @PostMapping("/config")
    public String updateSystemConfig(@RequestParam String cronSchedule,
                                   @RequestParam String kafkaTopic,
                                   @RequestParam int maxTopicsPerScan,
                                   RedirectAttributes redirectAttributes) {
        try {
            // In a real implementation, this would update ConfigMaps or trigger CronJob updates
            redirectAttributes.addFlashAttribute("successMessage", "Configuration updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating configuration: " + e.getMessage());
        }
        return "redirect:/admin/config";
    }
}
