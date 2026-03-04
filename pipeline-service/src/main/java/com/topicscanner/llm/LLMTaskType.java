package com.topicscanner.llm;

/**
 * Pipeline tasks that require LLM inference.
 * Each task type maps to a specific model in the provider config.
 */
public enum LLMTaskType {
    RELEVANCE,
    CLASSIFICATION,
    SUMMARIZATION,
    EMBEDDING,
    GENERATION
}
