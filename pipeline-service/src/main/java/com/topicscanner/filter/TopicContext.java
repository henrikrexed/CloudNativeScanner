package com.topicscanner.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context object carrying topic data through the filter pipeline.
 * Filters can read topic fields and write enrichment data to the attributes map.
 */
public class TopicContext {

    private final long topicId;
    private final String title;
    private final String url;
    private final String sourceType;
    private final String extractedContent;
    private final String categoryName;
    private final List<String> categoryKeywords;
    private final List<String> negativeKeywords;

    /** Mutable attributes bag — enrichment stages write results here. */
    private final Map<String, Object> attributes = new HashMap<>();

    public TopicContext(long topicId, String title, String url, String sourceType,
                        String extractedContent, String categoryName,
                        List<String> categoryKeywords, List<String> negativeKeywords) {
        this.topicId = topicId;
        this.title = title;
        this.url = url;
        this.sourceType = sourceType;
        this.extractedContent = extractedContent;
        this.categoryName = categoryName;
        this.categoryKeywords = categoryKeywords != null ? categoryKeywords : List.of();
        this.negativeKeywords = negativeKeywords != null ? negativeKeywords : List.of();
    }

    public long getTopicId() { return topicId; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getSourceType() { return sourceType; }
    public String getExtractedContent() { return extractedContent; }
    public String getCategoryName() { return categoryName; }
    public List<String> getCategoryKeywords() { return categoryKeywords; }
    public List<String> getNegativeKeywords() { return negativeKeywords; }
    public Map<String, Object> getAttributes() { return attributes; }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /** Combined text for analysis: title + extracted content. */
    public String getFullText() {
        if (extractedContent != null && !extractedContent.isBlank()) {
            return title + "\n\n" + extractedContent;
        }
        return title;
    }
}
