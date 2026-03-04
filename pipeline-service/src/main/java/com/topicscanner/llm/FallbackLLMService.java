package com.topicscanner.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Composite LLM service that tries the primary provider first,
 * then falls back to the configured cloud provider on failure.
 * <p>
 * Embeddings always prefer the primary provider (to avoid dimension mismatches).
 */
public class FallbackLLMService implements LLMService {

    private static final Logger logger = LoggerFactory.getLogger(FallbackLLMService.class);

    private final LLMService primary;
    private final LLMService fallback;

    public FallbackLLMService(LLMService primary, LLMService fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public String getProvider() {
        return primary.getProvider() + "+" + (fallback != null ? fallback.getProvider() : "none");
    }

    @Override
    public LLMResponse complete(LLMTaskType taskType, String systemPrompt, String userPrompt) {
        try {
            return primary.complete(taskType, systemPrompt, userPrompt);
        } catch (LLMException e) {
            logger.warn("Primary LLM ({}) failed for {}: {}. Trying fallback...",
                    primary.getProvider(), taskType, e.getMessage());

            if (fallback == null || !fallback.isAvailable()) {
                throw new LLMException(primary.getProvider(),
                        "Primary failed and no fallback available", e);
            }

            try {
                return fallback.complete(taskType, systemPrompt, userPrompt);
            } catch (LLMException fallbackEx) {
                throw new LLMException(getProvider(),
                        "Both primary and fallback failed", fallbackEx);
            }
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        // Prefer primary for embeddings to avoid dimension mismatches across providers.
        // Falls back only if primary fails (e.g., Ollama is down).
        try {
            return primary.embed(texts);
        } catch (Exception e) {
            logger.warn("Primary embedding ({}) failed: {}. Trying fallback...",
                    primary.getProvider(), e.getMessage());

            if (fallback == null || !fallback.isAvailable()) {
                throw new LLMException(primary.getProvider(),
                        "Primary embedding failed and no fallback available", e);
            }

            try {
                return fallback.embed(texts);
            } catch (Exception fallbackEx) {
                throw new LLMException(getProvider(),
                        "Both primary and fallback embedding failed", fallbackEx);
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return primary.isAvailable()
                || (fallback != null && fallback.isAvailable());
    }

    public LLMService getPrimary() {
        return primary;
    }

    public LLMService getFallback() {
        return fallback;
    }
}
