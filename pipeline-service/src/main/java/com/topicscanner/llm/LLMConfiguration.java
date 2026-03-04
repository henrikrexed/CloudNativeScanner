package com.topicscanner.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Wires up the LLM service beans based on application config.
 * Creates primary + fallback provider and wraps them in {@link FallbackLLMService}.
 */
@Configuration
public class LLMConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(LLMConfiguration.class);

    @Bean
    public LLMService llmService(LLMProperties properties, WebClient.Builder webClientBuilder) {
        LLMService primary = createProvider(properties.getPrimary(), properties, webClientBuilder);
        if (primary == null) {
            throw new IllegalStateException(
                    "Primary LLM provider '" + properties.getPrimary() + "' could not be created. "
                    + "Check topicscanner.llm.primary in application.yml");
        }

        LLMService fallback = createProvider(properties.getCloudFallback(), properties, webClientBuilder);

        logger.info("LLM configured: primary={}, fallback={}",
                primary.getProvider(), fallback != null ? fallback.getProvider() : "none");

        return new FallbackLLMService(primary, fallback);
    }

    private LLMService createProvider(String providerName, LLMProperties properties,
                                       WebClient.Builder webClientBuilder) {
        if (providerName == null || providerName.isBlank()) {
            return null;
        }

        LLMProperties.ProviderConfig config = properties.getProviderConfig(providerName);

        // Clone the builder so each provider gets its own independent WebClient
        return switch (providerName) {
            case "ollama" -> new OllamaLLMService(config, webClientBuilder.clone());
            case "openai" -> new OpenAILLMService(config, webClientBuilder.clone());
            case "claude" -> new ClaudeLLMService(config, webClientBuilder.clone());
            default -> {
                logger.warn("Unknown LLM provider: {}", providerName);
                yield null;
            }
        };
    }
}
