package com.topicscanner.llm;

/**
 * Thrown when an LLM provider returns HTTP 429 (rate limit).
 */
public class LLMRateLimitException extends LLMException {

    public LLMRateLimitException(String provider, String message) {
        super(provider, message);
    }

    public LLMRateLimitException(String provider, String message, Throwable cause) {
        super(provider, message, cause);
    }
}
