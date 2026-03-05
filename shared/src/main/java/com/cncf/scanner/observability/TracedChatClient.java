package com.cncf.scanner.observability;

import io.opentelemetry.api.GlobalOpenTelemetry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Wrapper for ChatClient that automatically adds OpenTelemetry tracing.
 * This provides OpenLLMetry-like functionality for Spring AI ChatClient.
 * 
 * Usage: 
 * 1. Inject ChatClient normally - it will be automatically wrapped with tracing
 * 2. For manual tracing, use: tracedChatClient.call(prompt) instead of chatClient.prompt(prompt).call().chatResponse()
 */
@Component
@ConditionalOnBean(ChatClient.class)
public class TracedChatClient {
    
    private final LLMTracingInterceptor interceptor;
    private final ChatClient chatClient;
    private final String provider;
    private final String model;
    private final Double temperature;
    
    @Autowired
    public TracedChatClient(
            ChatClient chatClient,
            @Value("${ai.provider:openai}") String provider,
            @Value("${spring.ai.openai.chat.options.model:#{null}}") String openaiModel,
            @Value("${spring.ai.anthropic.chat.options.model:#{null}}") String anthropicModel,
            @Value("${spring.ai.openai.chat.options.temperature:#{null}}") Double openaiTemp,
            @Value("${spring.ai.anthropic.chat.options.temperature:#{null}}") Double anthropicTemp) {
        this.chatClient = chatClient;
        this.provider = provider;
        // Determine model and temperature based on provider
        this.model = "anthropic".equalsIgnoreCase(provider) ? 
                (anthropicModel != null ? anthropicModel : "unknown") :
                (openaiModel != null ? openaiModel : "unknown");
        this.temperature = "anthropic".equalsIgnoreCase(provider) ? anthropicTemp : openaiTemp;
        this.interceptor = new LLMTracingInterceptor(
                GlobalOpenTelemetry.get(),
                "cloud-native-scanner"
        );
    }
    
    /**
     * Execute a prompt with automatic tracing.
     */
    public ChatResponse call(Prompt prompt) {
        return interceptor.executeWithTracing(chatClient, prompt, provider, model, temperature);
    }
    
    /**
     * Get the underlying ChatClient for advanced usage.
     */
    public ChatClient getChatClient() {
        return chatClient;
    }
}

