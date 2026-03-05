package com.topicscanner.filter;

/**
 * Result of a filter evaluation.
 */
public record FilterResult(
        boolean passed,
        String filterName,
        String reason,
        double score
) {

    public static FilterResult pass(String filterName) {
        return new FilterResult(true, filterName, null, 1.0);
    }

    public static FilterResult pass(String filterName, double score) {
        return new FilterResult(true, filterName, null, score);
    }

    public static FilterResult fail(String filterName, String reason) {
        return new FilterResult(false, filterName, reason, 0.0);
    }
}
