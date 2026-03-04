package com.topicscanner.llm;

/**
 * Exception thrown when an LLM operation fails.
 */
public class LLMException extends RuntimeException {

    private final String provider;

    public LLMException(String provider, String message) {
        super("[" + provider + "] " + message);
        this.provider = provider;
    }

    public LLMException(String provider, String message, Throwable cause) {
        super("[" + provider + "] " + message, cause);
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }
}
