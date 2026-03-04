package com.topicscanner.llm;

import java.util.List;

/**
 * Abstraction over LLM providers (Ollama, OpenAI, Claude).
 * <p>
 * Each implementation wraps a single provider. The {@link FallbackLLMService}
 * composes a primary + fallback to provide automatic retry across providers.
 */
public interface LLMService {

    /**
     * The provider identifier (e.g., "ollama", "openai", "claude").
     */
    String getProvider();

    /**
     * Send a completion request to the LLM.
     *
     * @param taskType determines which model to use from config
     * @param systemPrompt the system/instruction prompt
     * @param userPrompt the user/content prompt
     * @return LLM response with generated text and token usage
     * @throws LLMException on provider errors
     */
    LLMResponse complete(LLMTaskType taskType, String systemPrompt, String userPrompt);

    /**
     * Generate embeddings for the given texts.
     *
     * @param texts the texts to embed
     * @return list of embedding vectors (one per input text)
     * @throws LLMException on provider errors
     */
    List<float[]> embed(List<String> texts);

    /**
     * Check if this provider is available and configured.
     */
    boolean isAvailable();
}
