package com.topicscanner.filter.impl;

import com.topicscanner.filter.FilterResult;
import com.topicscanner.filter.TopicContext;
import com.topicscanner.filter.TopicFilter;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Filter 3: Rejects topics not in English (heuristic-based).
 * Order: 30 (cheap — character/word analysis only).
 * <p>
 * Uses a simple heuristic: checks for high ratio of ASCII characters
 * and presence of common English stop words.
 */
@Component
public class LanguageFilter implements TopicFilter {

    private static final double MIN_ASCII_RATIO = 0.85;
    private static final int MIN_STOP_WORD_MATCHES = 2;

    private static final Set<String> ENGLISH_STOP_WORDS = Set.of(
            "the", "is", "in", "and", "to", "of", "a", "for",
            "that", "this", "with", "are", "on", "it", "by",
            "from", "as", "be", "was", "an", "have", "has",
            "not", "but", "or", "can", "will", "how", "what"
    );

    private static final Pattern WORD_PATTERN = Pattern.compile("\\b[a-zA-Z]+\\b");

    @Override
    public String getName() {
        return "language";
    }

    @Override
    public int getOrder() {
        return 30;
    }

    @Override
    public FilterResult apply(TopicContext context) {
        String text = context.getFullText();
        if (text == null || text.isBlank()) {
            return FilterResult.pass(getName()); // No content to check
        }

        // Check ASCII ratio
        long asciiCount = text.chars().filter(c -> c < 128).count();
        double asciiRatio = (double) asciiCount / text.length();

        if (asciiRatio < MIN_ASCII_RATIO) {
            return FilterResult.fail(getName(),
                    String.format("Low ASCII ratio (%.0f%%), likely non-English", asciiRatio * 100));
        }

        // Check for English stop words
        String lower = text.toLowerCase();
        long stopWordMatches = ENGLISH_STOP_WORDS.stream()
                .filter(word -> {
                    // Match whole words only
                    int idx = lower.indexOf(word);
                    while (idx >= 0) {
                        boolean startOk = idx == 0 || !Character.isLetter(lower.charAt(idx - 1));
                        boolean endOk = idx + word.length() >= lower.length()
                                || !Character.isLetter(lower.charAt(idx + word.length()));
                        if (startOk && endOk) return true;
                        idx = lower.indexOf(word, idx + 1);
                    }
                    return false;
                })
                .count();

        if (stopWordMatches < MIN_STOP_WORD_MATCHES) {
            return FilterResult.fail(getName(),
                    "Too few English stop words found (" + stopWordMatches + ")");
        }

        return FilterResult.pass(getName(), asciiRatio);
    }
}
