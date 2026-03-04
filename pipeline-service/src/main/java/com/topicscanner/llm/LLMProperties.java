package com.topicscanner.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Configuration properties for LLM providers.
 * Maps to {@code llm.*} in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "topicscanner.llm")
public class LLMProperties {

    private String primary = "ollama";
    private String cloudFallback = "openai";

    private ProviderConfig ollama = new ProviderConfig();
    private ProviderConfig openai = new ProviderConfig();
    private ProviderConfig claude = new ProviderConfig();

    public String getPrimary() {
        return primary;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public String getCloudFallback() {
        return cloudFallback;
    }

    public void setCloudFallback(String cloudFallback) {
        this.cloudFallback = cloudFallback;
    }

    public ProviderConfig getOllama() {
        return ollama;
    }

    public void setOllama(ProviderConfig ollama) {
        this.ollama = ollama;
    }

    public ProviderConfig getOpenai() {
        return openai;
    }

    public void setOpenai(ProviderConfig openai) {
        this.openai = openai;
    }

    public ProviderConfig getClaude() {
        return claude;
    }

    public void setClaude(ProviderConfig claude) {
        this.claude = claude;
    }

    /**
     * Get the config for the named provider.
     */
    public ProviderConfig getProviderConfig(String provider) {
        return switch (provider) {
            case "ollama" -> ollama;
            case "openai" -> openai;
            case "claude" -> claude;
            default -> throw new IllegalArgumentException("Unknown LLM provider: " + provider);
        };
    }

    public static class ProviderConfig {
        private String url;
        private String apiKey;
        private int timeoutSeconds = 120;
        private Map<String, String> models = Map.of();

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public Map<String, String> getModels() {
            return models;
        }

        public void setModels(Map<String, String> models) {
            this.models = models;
        }

        /**
         * Get the model name for a given task type.
         */
        public String getModelForTask(LLMTaskType taskType) {
            String key = taskType.name().toLowerCase();
            String model = models.get(key);
            if (model == null || model.isBlank()) {
                throw new IllegalStateException("No model configured for task: " + key);
            }
            return model;
        }
    }
}
