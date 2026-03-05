package com.topicscanner.filter.impl;

import com.topicscanner.filter.FilterResult;
import com.topicscanner.filter.TopicContext;
import com.topicscanner.filter.TopicFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Filter 6: Scores content quality based on heuristics.
 * Order: 60 (moderate — text analysis, no LLM).
 * <p>
 * Quality signals: content length, paragraph count, code blocks,
 * headings, reading level indicators.
 */
@Component
public class QualityFilter implements TopicFilter {

    private final double qualityThreshold;

    public QualityFilter(
            @Value("${topicscanner.pipeline.quality-threshold:0.5}") double qualityThreshold) {
        this.qualityThreshold = qualityThreshold;
    }

    @Override
    public String getName() {
        return "quality";
    }

    @Override
    public int getOrder() {
        return 45;
    }

    @Override
    public FilterResult apply(TopicContext context) {
        String content = context.getExtractedContent();
        if (content == null || content.isBlank()) {
            return FilterResult.fail(getName(), "No content to score");
        }

        double score = calculateQualityScore(content);
        context.setAttribute("qualityScore", score);

        if (score < qualityThreshold) {
            return FilterResult.fail(getName(),
                    String.format("Quality score %.2f below threshold %.2f", score, qualityThreshold));
        }

        return FilterResult.pass(getName(), score);
    }

    double calculateQualityScore(String content) {
        double score = 0.0;
        int length = content.length();

        // Length score (0-0.3): longer content tends to be more substantive
        if (length > 5000) score += 0.3;
        else if (length > 2000) score += 0.2;
        else if (length > 500) score += 0.1;

        // Paragraph count (0-0.2): well-structured content has multiple paragraphs
        long paragraphs = content.split("\n\n").length;
        if (paragraphs >= 5) score += 0.2;
        else if (paragraphs >= 3) score += 0.15;
        else if (paragraphs >= 2) score += 0.1;

        // Code blocks (0-0.15): technical content often includes code
        long codeBlocks = countOccurrences(content, "```");
        if (codeBlocks >= 4) score += 0.15;  // 4 markers = 2 blocks
        else if (codeBlocks >= 2) score += 0.1;

        // Headings (0-0.15): organized content has headings
        long headings = content.lines().filter(l -> l.startsWith("##")).count();
        if (headings >= 3) score += 0.15;
        else if (headings >= 1) score += 0.1;

        // Lists (0-0.1): structured content uses lists
        long listItems = content.lines().filter(l -> l.trim().startsWith("- ") || l.trim().startsWith("* ")).count();
        if (listItems >= 3) score += 0.1;
        else if (listItems >= 1) score += 0.05;

        // Sentence variety (0-0.1): good writing has varying sentence length
        String[] sentences = content.split("[.!?]+");
        if (sentences.length >= 5) {
            double avgLen = (double) length / sentences.length;
            if (avgLen > 30 && avgLen < 200) score += 0.1;
        }

        return Math.min(1.0, score);
    }

    private long countOccurrences(String text, String sub) {
        long count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) >= 0) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
