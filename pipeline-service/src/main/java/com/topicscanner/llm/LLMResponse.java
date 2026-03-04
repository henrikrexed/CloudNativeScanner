package com.topicscanner.llm;

/**
 * Response from an LLM completion call.
 */
public record LLMResponse(
    String text,
    String model,
    int promptTokens,
    int completionTokens
) {

    public LLMResponse(String text, String model) {
        this(text, model, 0, 0);
    }
}
