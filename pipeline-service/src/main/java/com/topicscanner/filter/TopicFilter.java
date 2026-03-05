package com.topicscanner.filter;

/**
 * A single filter stage in the analysis pipeline.
 * Filters are applied cheapest-first to minimize LLM costs.
 */
public interface TopicFilter {

    /**
     * Display name for logging and metrics.
     */
    String getName();

    /**
     * Evaluation order (lower = earlier). Cheap filters should have low order.
     */
    int getOrder();

    /**
     * Evaluate a topic. Returns a result indicating pass/fail and reason.
     */
    FilterResult apply(TopicContext context);
}
