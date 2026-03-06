package com.topicscanner.telemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Metrics for LLM operations: rate limits and token usage.
 */
@Component
@ConditionalOnProperty(name = "topicscanner.telemetry.enabled", havingValue = "true", matchIfMissing = true)
public class LLMMetrics {

    private static final Logger log = LoggerFactory.getLogger(LLMMetrics.class);
    private static final AttributeKey<String> PROVIDER_KEY = AttributeKey.stringKey("provider");

    private final LongCounter rateLimitCounter;
    private final LongCounter inputTokenCounter;
    private final LongCounter outputTokenCounter;

    public LLMMetrics(TelemetryService telemetryService) {
        this.rateLimitCounter = telemetryService.createCounter(
                "topicscanner.llm.ratelimit",
                "Number of LLM rate limit (HTTP 429) responses");
        this.inputTokenCounter = telemetryService.createCounter(
                "topicscanner.llm.tokens.input",
                "Total input tokens consumed by LLM calls");
        this.outputTokenCounter = telemetryService.createCounter(
                "topicscanner.llm.tokens.output",
                "Total output tokens produced by LLM calls");
    }

    /**
     * Record a rate limit hit for the given provider.
     */
    public void recordRateLimit(String provider) {
        rateLimitCounter.add(1, Attributes.of(PROVIDER_KEY, provider));
        try {
            MDC.put("llm.provider", provider);
            MDC.put("llm.event", "rate_limit");
            log.warn("LLM rate limit (HTTP 429) hit for provider: {}", provider);
        } finally {
            MDC.remove("llm.provider");
            MDC.remove("llm.event");
        }
    }

    /**
     * Record token usage for the given provider.
     */
    public void recordTokenUsage(String provider, long inputTokens, long outputTokens) {
        Attributes attrs = Attributes.of(PROVIDER_KEY, provider);
        if (inputTokens > 0) {
            inputTokenCounter.add(inputTokens, attrs);
        }
        if (outputTokens > 0) {
            outputTokenCounter.add(outputTokens, attrs);
        }
    }
}
