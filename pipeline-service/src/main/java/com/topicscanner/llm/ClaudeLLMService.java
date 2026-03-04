package com.topicscanner.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * LLM service backed by the Anthropic Claude API.
 * Note: Claude does not offer an embedding endpoint — embedding calls
 * will throw an {@link UnsupportedOperationException}.
 */
public class ClaudeLLMService implements LLMService {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeLLMService.class);
    private static final String BASE_URL = "https://api.anthropic.com";
    private static final String API_VERSION = "2023-06-01";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final LLMProperties.ProviderConfig config;
    private final Duration timeout;

    public ClaudeLLMService(LLMProperties.ProviderConfig config, WebClient.Builder webClientBuilder) {
        this.config = config;
        this.timeout = Duration.ofSeconds(config.getTimeoutSeconds());
        this.webClient = webClientBuilder
                .baseUrl(BASE_URL)
                .defaultHeader("x-api-key", config.getApiKey())
                .defaultHeader("anthropic-version", API_VERSION)
                .build();
    }

    @Override
    public String getProvider() {
        return "claude";
    }

    @Override
    public LLMResponse complete(LLMTaskType taskType, String systemPrompt, String userPrompt) {
        String model = config.getModelForTask(taskType);
        logger.debug("Claude completion: model={}, task={}", model, taskType);

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 4096,
                "system", systemPrompt,
                "messages", List.of(
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            String responseJson = webClient.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(timeout);

            if (responseJson == null) {
                throw new LLMException("claude", "Empty response from Claude for model " + model);
            }

            JsonNode root = OBJECT_MAPPER.readTree(responseJson);
            String text = root.path("content").get(0).path("text").asText();
            int inputTokens = root.path("usage").path("input_tokens").asInt(0);
            int outputTokens = root.path("usage").path("output_tokens").asInt(0);

            return new LLMResponse(text, model, inputTokens, outputTokens);
        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            throw new LLMException("claude", "Completion failed for model " + model, e);
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        throw new UnsupportedOperationException(
                "Claude does not provide embeddings. Use Ollama or OpenAI for embedding tasks.");
    }

    @Override
    public boolean isAvailable() {
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }
}
