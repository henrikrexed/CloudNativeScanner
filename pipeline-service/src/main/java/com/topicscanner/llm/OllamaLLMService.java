package com.topicscanner.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.topicscanner.telemetry.GenAISpanHelper;
import com.topicscanner.telemetry.LLMMetrics;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
    private final Tracer tracer;
    private final LLMMetrics llmMetrics;

    public OllamaLLMService(LLMProperties.ProviderConfig config, WebClient.Builder webClientBuilder,
                             Tracer tracer, LLMMetrics llmMetrics) {
        this.config = config;
        this.timeout = Duration.ofSeconds(config.getTimeoutSeconds());
        this.tracer = tracer;
        this.llmMetrics = llmMetrics;
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

        return GenAISpanHelper.traceCompletion(tracer, "ollama", model, systemPrompt, userPrompt,
                () -> doComplete(model, systemPrompt, userPrompt),
                new GenAISpanHelper.TokenExtractor<>() {
                    @Override public String model(LLMResponse r) { return r.model(); }
                    @Override public String text(LLMResponse r) { return r.text(); }
                    @Override public long inputTokens(LLMResponse r) { return r.promptTokens(); }
                    @Override public long outputTokens(LLMResponse r) { return r.completionTokens(); }
                });
    }

    private LLMResponse doComplete(String model, String systemPrompt, String userPrompt) {
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

            LLMResponse response = new LLMResponse(text, model, promptTokens, completionTokens);
            if (llmMetrics != null) {
                llmMetrics.recordTokenUsage("ollama", promptTokens, completionTokens);
            }
            return response;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                if (llmMetrics != null) llmMetrics.recordRateLimit("ollama");
                throw new LLMRateLimitException("ollama", "Rate limited by Ollama", e);
            }
            throw new LLMException("ollama", "Completion failed for model " + model, e);
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

        return GenAISpanHelper.traceEmbedding(tracer, "ollama", model, texts.size(),
                () -> doEmbed(model, texts));
    }

    private List<float[]> doEmbed(String model, List<String> texts) {
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
            } catch (WebClientResponseException e) {
                if (e.getStatusCode().value() == 429) {
                    if (llmMetrics != null) llmMetrics.recordRateLimit("ollama");
                    throw new LLMRateLimitException("ollama", "Rate limited by Ollama", e);
                }
                throw new LLMException("ollama", "Embedding failed for model " + model, e);
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
