package com.cncf.scanner.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
// Note: Using custom attribute keys instead of SemanticAttributes for LLM-specific attributes
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Duration;
import java.time.Instant;

/**
 * Interceptor for tracing LLM interactions using OpenTelemetry LLM semantic conventions.
 * This provides OpenLLMetry-like functionality for Java/Spring Boot applications.
 * 
 * Uses OpenTelemetry LLM semantic conventions:
 * - llm.request.type
 * - llm.request.model
 * - llm.request.temperature
 * - llm.response.model
 * - llm.usage.prompt_tokens
 * - llm.usage.completion_tokens
 * - llm.usage.total_tokens
 */
public class LLMTracingInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMTracingInterceptor.class);
    
    // OpenTelemetry LLM Semantic Convention attribute keys (using standard semantic conventions)
    // These follow the OpenTelemetry LLM semantic conventions
    private static final AttributeKey<String> LLM_REQUEST_TYPE = AttributeKey.stringKey("gen_ai.request.type");
    private static final AttributeKey<String> LLM_REQUEST_MODEL = AttributeKey.stringKey("gen_ai.request.model");
    private static final AttributeKey<Double> LLM_REQUEST_TEMPERATURE = AttributeKey.doubleKey("gen_ai.request.temperature");
    // Note: Response model can be extracted from metadata if available
    private static final AttributeKey<Long> LLM_USAGE_PROMPT_TOKENS = AttributeKey.longKey("gen_ai.usage.prompt_tokens");
    private static final AttributeKey<Long> LLM_USAGE_COMPLETION_TOKENS = AttributeKey.longKey("gen_ai.usage.completion_tokens");
    private static final AttributeKey<Long> LLM_USAGE_TOTAL_TOKENS = AttributeKey.longKey("gen_ai.usage.total_tokens");
    private static final AttributeKey<String> LLM_PROVIDER = AttributeKey.stringKey("gen_ai.system");
    private static final AttributeKey<String> LLM_PROMPT = AttributeKey.stringKey("gen_ai.prompt");
    private static final AttributeKey<String> LLM_COMPLETION = AttributeKey.stringKey("gen_ai.completion");
    
    private final Tracer tracer;
    private final String serviceName;
    
    public LLMTracingInterceptor(OpenTelemetry openTelemetry, String serviceName) {
        this.tracer = openTelemetry.getTracer("cloud-native-scanner-llm", "1.0.0");
        this.serviceName = serviceName;
    }
    
    /**
     * Execute a ChatClient call with tracing.
     */
    public ChatResponse executeWithTracing(ChatClient chatClient, Prompt prompt, String provider, String model) {
        return executeWithTracing(chatClient, prompt, provider, model, null);
    }
    
    /**
     * Execute a ChatClient call with tracing, including temperature.
     * This method wraps the ChatClient.prompt(prompt).call().chatResponse() pattern.
     */
    public ChatResponse executeWithTracing(ChatClient chatClient, Prompt prompt, String provider, String model, Double temperature) {
        Instant startTime = Instant.now();
        Span span = null;
        Scope scope = null;
        
        try {
            // Create span for LLM request
            span = tracer.spanBuilder("llm.chat")
                    .setSpanKind(SpanKind.CLIENT)
                    .setAttribute(AttributeKey.stringKey("service.name"), serviceName)
                    .setAttribute(LLM_REQUEST_TYPE, "chat")
                    .setAttribute(LLM_PROVIDER, provider != null ? provider : "unknown")
                    .setAttribute(LLM_REQUEST_MODEL, model != null ? model : "unknown")
                    .startSpan();
            
            if (temperature != null) {
                span.setAttribute(LLM_REQUEST_TEMPERATURE, temperature);
            }
            
            // Add prompt information
            if (prompt != null && prompt.getInstructions() != null && !prompt.getInstructions().isEmpty()) {
                // Get the first message content
                String promptText = prompt.getInstructions().stream()
                        .filter(msg -> msg.getContent() != null)
                        .map(msg -> msg.getContent())
                        .findFirst()
                        .orElse("");
                if (promptText != null && promptText.length() > 0) {
                    // Truncate for attribute (limit to 1000 chars to avoid too large attributes)
                    String truncatedPrompt = promptText.length() > 1000 
                            ? promptText.substring(0, 1000) + "..." 
                            : promptText;
                    span.setAttribute(LLM_PROMPT, truncatedPrompt);
                }
            }
            
            // Make the actual call
            scope = span.makeCurrent();
            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
            
            // Record response information
            if (response != null) {
                if (response.getResult() != null && response.getResult().getOutput() != null) {
                    String output = response.getResult().getOutput().getContent();
                    if (output != null && output.length() > 0) {
                        // Truncate for attribute
                        String truncatedOutput = output.length() > 1000 
                                ? output.substring(0, 1000) + "..." 
                                : output;
                        span.setAttribute(LLM_COMPLETION, truncatedOutput);
                    }
                }
                
                // Extract token usage if available from metadata
                if (response.getMetadata() != null) {
                    try {
                        // Try to access usage information from metadata
                        // Spring AI metadata structure may vary by provider
                        var usage = response.getMetadata().getUsage();
                        if (usage != null) {
                            if (usage.getPromptTokens() != null) {
                                span.setAttribute(LLM_USAGE_PROMPT_TOKENS, usage.getPromptTokens().longValue());
                            }
                            if (usage.getGenerationTokens() != null) {
                                span.setAttribute(LLM_USAGE_COMPLETION_TOKENS, usage.getGenerationTokens().longValue());
                            }
                            if (usage.getTotalTokens() != null) {
                                span.setAttribute(LLM_USAGE_TOTAL_TOKENS, usage.getTotalTokens().longValue());
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("Could not extract token usage from metadata: {}", e.getMessage());
                    }
                }
            }
            
            span.setStatus(StatusCode.OK);
            return response;
            
        } catch (Exception e) {
            if (span != null) {
                span.setStatus(StatusCode.ERROR, e.getMessage());
                span.recordException(e);
            }
            logger.error("Error in LLM call: {}", e.getMessage(), e);
            throw e;
        } finally {
            if (scope != null) {
                scope.close();
            }
            if (span != null) {
                Duration duration = Duration.between(startTime, Instant.now());
                span.setAttribute(AttributeKey.longKey("duration_ms"), duration.toMillis());
                span.end();
            }
        }
    }
    
}

