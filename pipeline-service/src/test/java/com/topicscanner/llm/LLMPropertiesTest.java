package com.topicscanner.llm;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LLMPropertiesTest {

    @Test
    void getModelForTask_existingTask() {
        var config = new LLMProperties.ProviderConfig();
        config.setModels(Map.of(
                "relevance", "qwen2.5:14b",
                "classification", "qwen2.5:14b",
                "summarization", "qwen2.5:32b",
                "embedding", "nomic-embed-text",
                "generation", "qwen2.5:32b"
        ));

        assertEquals("qwen2.5:14b", config.getModelForTask(LLMTaskType.RELEVANCE));
        assertEquals("nomic-embed-text", config.getModelForTask(LLMTaskType.EMBEDDING));
        assertEquals("qwen2.5:32b", config.getModelForTask(LLMTaskType.GENERATION));
    }

    @Test
    void getModelForTask_missingTask_throws() {
        var config = new LLMProperties.ProviderConfig();
        config.setModels(Map.of());

        assertThrows(IllegalStateException.class,
                () -> config.getModelForTask(LLMTaskType.RELEVANCE));
    }

    @Test
    void getProviderConfig_validProviders() {
        var props = new LLMProperties();

        assertNotNull(props.getProviderConfig("ollama"));
        assertNotNull(props.getProviderConfig("openai"));
        assertNotNull(props.getProviderConfig("claude"));
    }

    @Test
    void getProviderConfig_unknownProvider_throws() {
        var props = new LLMProperties();

        assertThrows(IllegalArgumentException.class,
                () -> props.getProviderConfig("gemini"));
    }

    @Test
    void defaults() {
        var props = new LLMProperties();

        assertEquals("ollama", props.getPrimary());
        assertEquals("openai", props.getCloudFallback());
    }

    @Test
    void timeoutSeconds_default() {
        var config = new LLMProperties.ProviderConfig();
        assertEquals(120, config.getTimeoutSeconds());
    }

    @Test
    void timeoutSeconds_configurable() {
        var config = new LLMProperties.ProviderConfig();
        config.setTimeoutSeconds(60);
        assertEquals(60, config.getTimeoutSeconds());
    }

    @Test
    void allTaskTypes_canBeLookedUp() {
        var config = new LLMProperties.ProviderConfig();
        config.setModels(Map.of(
                "relevance", "model-a",
                "classification", "model-b",
                "summarization", "model-c",
                "embedding", "model-d",
                "generation", "model-e"
        ));

        for (LLMTaskType type : LLMTaskType.values()) {
            assertNotNull(config.getModelForTask(type));
        }
    }
}
