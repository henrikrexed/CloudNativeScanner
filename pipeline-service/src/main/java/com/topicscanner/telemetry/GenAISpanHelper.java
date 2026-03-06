package com.topicscanner.telemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import java.util.function.Supplier;

/**
 * Helper for creating OpenTelemetry spans following GenAI semantic conventions.
 */
public final class GenAISpanHelper {

    // GenAI semantic convention attribute keys
    public static final AttributeKey<String> GEN_AI_SYSTEM = AttributeKey.stringKey("gen_ai.system");
    public static final AttributeKey<String> GEN_AI_REQUEST_MODEL = AttributeKey.stringKey("gen_ai.request.model");
    public static final AttributeKey<String> GEN_AI_RESPONSE_MODEL = AttributeKey.stringKey("gen_ai.response.model");
    public static final AttributeKey<Long> GEN_AI_USAGE_INPUT_TOKENS = AttributeKey.longKey("gen_ai.usage.input_tokens");
    public static final AttributeKey<Long> GEN_AI_USAGE_OUTPUT_TOKENS = AttributeKey.longKey("gen_ai.usage.output_tokens");
    public static final AttributeKey<Long> GEN_AI_USAGE_TOTAL_TOKENS = AttributeKey.longKey("gen_ai.usage.total_tokens");

    public static final String COMPLETION_SPAN = "gen_ai.chat.completions";
    public static final String EMBEDDING_SPAN = "gen_ai.embeddings";
    public static final String PROMPT_EVENT = "gen_ai.content.prompt";
    public static final String COMPLETION_EVENT = "gen_ai.content.completion";

    private GenAISpanHelper() {}

    /**
     * Execute a completion call within a GenAI span.
     */
    public static <T> T traceCompletion(Tracer tracer, String provider, String model,
                                         String systemPrompt, String userPrompt,
                                         Supplier<T> operation,
                                         TokenExtractor<T> tokenExtractor) {
        Span span = tracer.spanBuilder(COMPLETION_SPAN)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(GEN_AI_SYSTEM, provider)
                .setAttribute(GEN_AI_REQUEST_MODEL, model)
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            // Record prompt as event
            span.addEvent(PROMPT_EVENT, Attributes.of(
                    AttributeKey.stringKey("gen_ai.prompt.system"), truncate(systemPrompt, 1000),
                    AttributeKey.stringKey("gen_ai.prompt.user"), truncate(userPrompt, 1000)
            ));

            T result = operation.get();

            // Record response attributes
            span.setAttribute(GEN_AI_RESPONSE_MODEL, tokenExtractor.model(result));
            long inputTokens = tokenExtractor.inputTokens(result);
            long outputTokens = tokenExtractor.outputTokens(result);
            span.setAttribute(GEN_AI_USAGE_INPUT_TOKENS, inputTokens);
            span.setAttribute(GEN_AI_USAGE_OUTPUT_TOKENS, outputTokens);
            span.setAttribute(GEN_AI_USAGE_TOTAL_TOKENS, inputTokens + outputTokens);

            // Record completion as event
            span.addEvent(COMPLETION_EVENT, Attributes.of(
                    AttributeKey.stringKey("gen_ai.completion"), truncate(tokenExtractor.text(result), 1000)
            ));

            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Execute an embedding call within a GenAI span.
     */
    public static <T> T traceEmbedding(Tracer tracer, String provider, String model,
                                         int inputCount, Supplier<T> operation) {
        Span span = tracer.spanBuilder(EMBEDDING_SPAN)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(GEN_AI_SYSTEM, provider)
                .setAttribute(GEN_AI_REQUEST_MODEL, model)
                .setAttribute(AttributeKey.longKey("gen_ai.embedding.input_count"), (long) inputCount)
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            T result = operation.get();
            span.setAttribute(GEN_AI_RESPONSE_MODEL, model);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    /**
     * Extracts token usage info from a response object.
     */
    public interface TokenExtractor<T> {
        String model(T result);
        String text(T result);
        long inputTokens(T result);
        long outputTokens(T result);
    }
}
