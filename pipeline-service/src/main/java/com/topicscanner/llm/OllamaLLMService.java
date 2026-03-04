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
 * LLM service backed by a local Ollama instance.
 */
public class OllamaLLMService implements LLMService {

    private static final Logger logger = LoggerFactory.getLogger(OllamaLLMService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final LLMProperties.ProviderConfig config;
    private final Duration timeout;

    public OllamaLLMService(LLMProperties.ProviderConfig config, WebClient.Builder webClientBuilder) {
        this.config = config;
        this.timeout = Duration.ofSeconds(config.getTimeoutSeconds());
        String url = config.getUrl() != null ? config.getUrl() : "http://localhost:11434";
        this.webClient = webClientBuilder.baseUrl(url).build();
    }

    @Override
    public String getProvider() {
        return "ollama";
    }

    @Override
    public LLMResponse complete(LLMTaskType taskType, String systemPrompt, String userPrompt) {
        String model = config.getModelForTask(taskType);
        logger.debug("Ollama completion: model={}, task={}", model, taskType);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "stream", false
        );

        try {
            String responseJson = webClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(timeout);

            if (responseJson == null) {
                throw new LLMException("ollama", "Empty response from Ollama for model " + model);
            }

            JsonNode root = OBJECT_MAPPER.readTree(responseJson);
            String text = root.path("message").path("content").asText();
            int promptTokens = root.path("prompt_eval_count").asInt(0);
            int completionTokens = root.path("eval_count").asInt(0);

            return new LLMResponse(text, model, promptTokens, completionTokens);
        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            throw new LLMException("ollama", "Completion failed for model " + model, e);
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        String model = config.getModelForTask(LLMTaskType.EMBEDDING);
        logger.debug("Ollama embedding: model={}, texts={}", model, texts.size());

        List<float[]> embeddings = new ArrayList<>();
        for (String text : texts) {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "input", text
            );

            try {
                String responseJson = webClient.post()
                        .uri("/api/embed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(timeout);

                if (responseJson == null) {
                    throw new LLMException("ollama", "Empty embedding response for model " + model);
                }

                JsonNode root = OBJECT_MAPPER.readTree(responseJson);
                JsonNode embeddingsNode = root.path("embeddings").get(0);
                float[] vector = new float[embeddingsNode.size()];
                for (int i = 0; i < embeddingsNode.size(); i++) {
                    vector[i] = (float) embeddingsNode.get(i).asDouble();
                }
                embeddings.add(vector);
            } catch (LLMException e) {
                throw e;
            } catch (Exception e) {
                throw new LLMException("ollama", "Embedding failed for model " + model, e);
            }
        }
        return embeddings;
    }

    @Override
    public boolean isAvailable() {
        try {
            webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(5));
            return true;
        } catch (Exception e) {
            logger.debug("Ollama not available: {}", e.getMessage());
            return false;
        }
    }
}
