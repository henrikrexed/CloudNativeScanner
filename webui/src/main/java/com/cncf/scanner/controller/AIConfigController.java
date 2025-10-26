package com.cncf.scanner.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/ai-config")
public class AIConfigController {
    
    @Value("${ai.enabled:true}")
    private boolean aiEnabled;
    
    @Value("${ai.similarity.threshold:0.8}")
    private double similarityThreshold;
    
    @Value("${ai.confidence.threshold:0.7}")
    private double confidenceThreshold;
    
    @Value("${ai.model:gpt-3.5-turbo}")
    private String aiModel;
    
    @GetMapping
    public String aiConfig(Model model) {
        model.addAttribute("aiEnabled", aiEnabled);
        model.addAttribute("similarityThreshold", similarityThreshold);
        model.addAttribute("confidenceThreshold", confidenceThreshold);
        model.addAttribute("aiModel", aiModel);
        
        return "admin/ai-config";
    }
    
    @PostMapping
    public String updateAIConfig(@RequestParam boolean aiEnabled,
                                @RequestParam double similarityThreshold,
                                @RequestParam double confidenceThreshold,
                                @RequestParam String aiModel,
                                RedirectAttributes redirectAttributes) {
        try {
            // In a real implementation, this would update ConfigMaps or trigger service restarts
            redirectAttributes.addFlashAttribute("successMessage", "AI configuration updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating AI configuration: " + e.getMessage());
        }
        
        return "redirect:/admin/ai-config";
    }
}


