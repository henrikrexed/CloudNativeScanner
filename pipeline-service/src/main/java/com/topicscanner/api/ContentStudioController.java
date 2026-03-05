package com.topicscanner.api;

import com.topicscanner.generator.ContentGenerationService;
import com.topicscanner.generator.StyleAnalysisService;
import com.topicscanner.generator.UserContentService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Content Studio REST API — user content upload, style analysis, and content generation.
 * Serves the Content Studio view in the Next.js UI.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ContentStudioController {

    private final UserContentService userContentService;
    private final StyleAnalysisService styleAnalysisService;
    private final ContentGenerationService contentGenerationService;

    public ContentStudioController(UserContentService userContentService,
                                    StyleAnalysisService styleAnalysisService,
                                    ContentGenerationService contentGenerationService) {
        this.userContentService = userContentService;
        this.styleAnalysisService = styleAnalysisService;
        this.contentGenerationService = contentGenerationService;
    }

    // ---- User Content Library ----

    /**
     * GET /api/content — list uploaded content library.
     */
    @GetMapping("/content")
    public List<Map<String, Object>> listContent() {
        return userContentService.listContent();
    }

    /**
     * GET /api/content/{id} — get a single content item.
     */
    @GetMapping("/content/{id}")
    public ResponseEntity<Map<String, Object>> getContent(@PathVariable Long id) {
        Map<String, Object> content = userContentService.getContent(id);
        return content != null ? ResponseEntity.ok(content) : ResponseEntity.notFound().build();
    }

    /**
     * POST /api/content — upload content directly.
     * Body: { "title": "...", "content": "...", "contentType": "blog_post|transcript|newsletter" }
     */
    @PostMapping("/content")
    public ResponseEntity<Map<String, Object>> uploadContent(@RequestBody Map<String, String> body) {
        try {
            Long id = userContentService.uploadContent(
                    body.get("title"),
                    body.get("content"),
                    body.getOrDefault("contentType", "blog_post"));
            return ResponseEntity.status(201).body(Map.of("id", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/content/import — import content from a URL.
     * Body: { "url": "https://..." }
     */
    @PostMapping("/content/import")
    public ResponseEntity<Map<String, Object>> importContent(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "url is required"));
        }
        try {
            Long id = userContentService.importFromUrl(url);
            return ResponseEntity.status(201).body(Map.of("id", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/content/{id} — delete uploaded content.
     */
    @DeleteMapping("/content/{id}")
    public ResponseEntity<Void> deleteContent(@PathVariable Long id) {
        return userContentService.deleteContent(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // ---- Style Analysis ----

    /**
     * POST /api/content/{id}/analyze — trigger style analysis for a content item.
     */
    @PostMapping("/content/{id}/analyze")
    public ResponseEntity<Map<String, Object>> analyzeStyle(@PathVariable Long id) {
        try {
            String styleMetadata = styleAnalysisService.analyzeContent(id);
            return ResponseEntity.ok(Map.of("id", id, "styleMetadata", styleMetadata));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/content/style-profile — get composite style profile.
     */
    @GetMapping("/content/style-profile")
    public ResponseEntity<Map<String, Object>> getStyleProfile() {
        String profile = styleAnalysisService.getCompositeStyleProfile();
        if (profile == null) {
            return ResponseEntity.ok(Map.of("profile", "", "hasProfile", false));
        }
        return ResponseEntity.ok(Map.of("profile", profile, "hasProfile", true));
    }

    // ---- Content Generation ----

    /**
     * POST /api/generate — generate content from a topic.
     * Body: { "topicId": 123, "groupId": "...", "outputFormat": "blog_post|youtube_script|linkedin_post|newsletter" }
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateContent(@RequestBody Map<String, Object> body) {
        try {
            Long topicId = body.get("topicId") != null
                    ? ((Number) body.get("topicId")).longValue() : null;
            String groupId = (String) body.get("groupId");
            String outputFormat = (String) body.getOrDefault("outputFormat", "blog_post");

            if (topicId == null && groupId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "topicId or groupId is required"));
            }

            Long id = contentGenerationService.generate(topicId, groupId, outputFormat);
            Map<String, Object> result = contentGenerationService.getGeneratedContent(id);
            return ResponseEntity.status(201).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/generate — list generated content.
     */
    @GetMapping("/generate")
    public List<Map<String, Object>> listGenerated(@RequestParam(required = false) Long topicId) {
        return contentGenerationService.listGeneratedContent(topicId);
    }

    /**
     * GET /api/generate/{id} — get generated content.
     */
    @GetMapping("/generate/{id}")
    public ResponseEntity<Map<String, Object>> getGenerated(@PathVariable Long id) {
        Map<String, Object> content = contentGenerationService.getGeneratedContent(id);
        return content != null ? ResponseEntity.ok(content) : ResponseEntity.notFound().build();
    }

    /**
     * POST /api/generate/{id}/regenerate — regenerate with feedback.
     * Body: { "feedback": "make it more technical" }
     */
    @PostMapping("/generate/{id}/regenerate")
    public ResponseEntity<Map<String, Object>> regenerate(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        String feedback = body.get("feedback");
        if (feedback == null || feedback.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "feedback is required"));
        }
        try {
            Long newId = contentGenerationService.regenerate(id, feedback);
            Map<String, Object> result = contentGenerationService.getGeneratedContent(newId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(429).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/generate/{id}/export — export as Markdown.
     */
    @GetMapping("/generate/{id}/export")
    public ResponseEntity<String> exportContent(
            @PathVariable Long id,
            @RequestParam(defaultValue = "markdown") String format) {
        String markdown = contentGenerationService.exportAsMarkdown(id);
        if (markdown == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_MARKDOWN);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("generated-content-" + id + ".md")
                .build());

        return new ResponseEntity<>(markdown, headers, HttpStatus.OK);
    }
}
