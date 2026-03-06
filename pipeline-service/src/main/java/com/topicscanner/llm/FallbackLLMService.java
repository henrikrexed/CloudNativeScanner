package com.topicscanner.llm;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Composite LLM service that tries the primary provider first,
 * then falls back to the configured cloud provider on failure.
 * Wraps the entire fallback chain in a parent span for observability.
 */
public class FallbackLLMService implements LLMService {

    private static final Logger logger = LoggerFactory.getLogger(FallbackLLMService.class);

    private final LLMService primary;
    private final LLMService fallback;
    private final Tracer tracer;

    public FallbackLLMService(LLMService primary, LLMService fallback, Tracer tracer) {
        this.primary = primary;
        this.fallback = fallback;
        this.tracer = tracer;
    }

    @Override
    public String getProvider() {
        return primary.getProvider() + "+" + (fallback != null ? fallback.getProvider() : "none");
    }

    @Override
    public LLMResponse complete(LLMTaskType taskType, String systemPrompt, String userPrompt) {
        Span span = tracer.spanBuilder("llm.fallback.complete")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute(AttributeKey.stringKey("llm.primary"), primary.getProvider())
                .setAttribute(AttributeKey.stringKey("llm.fallback"),
                        fallback != null ? fallback.getProvider() : "none")
                .setAttribute(AttributeKey.stringKey("llm.task"), taskType.name())
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            LLMResponse result = primary.complete(taskType, systemPrompt, userPrompt);
            span.setAttribute(AttributeKey.stringKey("llm.resolved_provider"), primary.getProvider());
            return result;
        } catch (LLMException e) {
            span.addEvent("primary_failed");
            logger.warn("Primary LLM ({}) failed for {}: {}. Trying fallback...",
                    primary.getProvider(), taskType, e.getMessage());

            if (fallback == null || !fallback.isAvailable()) {
                span.setStatus(StatusCode.ERROR, "Primary failed, no fallback available");
                throw new LLMException(primary.getProvider(),
                        "Primary failed and no fallback available", e);
            }

            try {
                LLMResponse result = fallback.complete(taskType, systemPrompt, userPrompt);
                span.setAttribute(AttributeKey.stringKey("llm.resolved_provider"), fallback.getProvider());
                span.addEvent("fallback_succeeded");
                return result;
            } catch (LLMException fallbackEx) {
                span.setStatus(StatusCode.ERROR, "Both primary and fallback failed");
                throw new LLMException(getProvider(),
                        "Both primary and fallback failed", fallbackEx);
            }
        } finally {
            span.end();
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        Span span = tracer.spanBuilder("llm.fallback.embed")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute(AttributeKey.stringKey("llm.primary"), primary.getProvider())
                .setAttribute(AttributeKey.stringKey("llm.fallback"),
                        fallback != null ? fallback.getProvider() : "none")
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            List<float[]> result = primary.embed(texts);
            span.setAttribute(AttributeKey.stringKey("llm.resolved_provider"), primary.getProvider());
            return result;
        } catch (Exception e) {
            span.addEvent("primary_embedding_failed");
            logger.warn("Primary embedding ({}) failed: {}. Trying fallback...",
                    primary.getProvider(), e.getMessage());

            if (fallback == null || !fallback.isAvailable()) {
                span.setStatus(StatusCode.ERROR, "Primary embedding failed, no fallback");
                throw new LLMException(primary.getProvider(),
                        "Primary embedding failed and no fallback available", e);
            }

            try {
                List<float[]> result = fallback.embed(texts);
                span.setAttribute(AttributeKey.stringKey("llm.resolved_provider"), fallback.getProvider());
                span.addEvent("fallback_embedding_succeeded");
                return result;
            } catch (Exception fallbackEx) {
                span.setStatus(StatusCode.ERROR, "Both primary and fallback embedding failed");
                throw new LLMException(getProvider(),
                        "Both primary and fallback embedding failed", fallbackEx);
            }
        } finally {
            span.end();
        }
    }

    @Override
    public boolean isAvailable() {
        return primary.isAvailable()
                || (fallback != null && fallback.isAvailable());
    }

    public LLMService getPrimary() {
        return primary;
    }

    public LLMService getFallback() {
        return fallback;
    }
}
