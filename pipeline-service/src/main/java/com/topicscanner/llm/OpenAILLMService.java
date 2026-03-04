package com.topicscanner.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * LLM service backed by the OpenAI API.
 */
public class OpenAILLMService implements LLMService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAILLMService.class);
    private static final String BASE_URL = "https://api.openai.com/v1";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final LLMProperties.ProviderConfig config;
    private final Duration timeout;

    public OpenAILLMService(LLMProperties.ProviderConfig config, WebClient.Builder webClientBuilder) {
        this.config = config;
        this.timeout = Duration.ofSeconds(config.getTimeoutSeconds());
        this.webClient = webClientBuilder
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .build();
    }

    @Override
    public String getProvider() {
        return "openai";
    }

    @Override
    public LLMResponse complete(LLMTaskType taskType, String systemPrompt, String userPrompt) {
        String model = config.getModelForTask(taskType);
        logger.debug("OpenAI completion: model={}, task={}", model, taskType);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            String responseJson = webClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(timeout);

            if (responseJson == null) {
                throw new LLMException("openai", "Empty response from OpenAI for model " + model);
            }

            JsonNode root = OBJECT_MAPPER.readTree(responseJson);
            String text = root.path("choices").get(0).path("message").path("content").asText();
            int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
            int completionTokens = root.path("usage").path("completion_tokens").asInt(0);

            return new LLMResponse(text, model, promptTokens, completionTokens);
        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            throw new LLMException("openai", "Completion failed for model " + model, e);
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        String model = config.getModelForTask(LLMTaskType.EMBEDDING);
        logger.debug("OpenAI embedding: model={}, texts={}", model, texts.size());

        Map<String, Object> body = Map.of(
                "model", model,
                "input", texts
        );

        try {
            String responseJson = webClient.post()
                    .uri("/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(timeout);

            if (responseJson == null) {
                throw new LLMException("openai", "Empty embedding response for model " + model);
            }

            JsonNode root = OBJECT_MAPPER.readTree(responseJson);
            JsonNode data = root.path("data");
            List<float[]> embeddings = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode embeddingNode = item.path("embedding");
                float[] vector = new float[embeddingNode.size()];
                for (int i = 0; i < embeddingNode.size(); i++) {
                    vector[i] = (float) embeddingNode.get(i).asDouble();
                }
                embeddings.add(vector);
            }
            return embeddings;
        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            throw new LLMException("openai", "Embedding failed for model " + model, e);
        }
    }

    @Override
    public boolean isAvailable() {
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }
}
