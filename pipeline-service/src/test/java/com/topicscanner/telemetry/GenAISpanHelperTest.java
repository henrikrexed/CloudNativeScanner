package com.topicscanner.telemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenAISpanHelperTest {

    private Tracer tracer;
    private InMemorySpanExporter spanExporter;

    @BeforeEach
    void setUp() {
        spanExporter = InMemorySpanExporter.create();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();
        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
        tracer = sdk.getTracer("test");
    }

    @Test
    void traceCompletion_createsSpanWithAttributes() {
        String result = GenAISpanHelper.traceCompletion(
                tracer, "openai", "gpt-4", "system prompt", "user prompt",
                () -> "hello world",
                new GenAISpanHelper.TokenExtractor<>() {
                    @Override public String model(String r) { return "gpt-4"; }
                    @Override public String text(String r) { return r; }
                    @Override public long inputTokens(String r) { return 10; }
                    @Override public long outputTokens(String r) { return 5; }
                });

        assertEquals("hello world", result);

        var spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());

        SpanData span = spans.get(0);
        assertEquals("gen_ai.chat.completions", span.getName());
        assertEquals("openai", span.getAttributes().get(GenAISpanHelper.GEN_AI_SYSTEM));
        assertEquals("gpt-4", span.getAttributes().get(GenAISpanHelper.GEN_AI_REQUEST_MODEL));
        assertEquals("gpt-4", span.getAttributes().get(GenAISpanHelper.GEN_AI_RESPONSE_MODEL));
        assertEquals(10L, span.getAttributes().get(GenAISpanHelper.GEN_AI_USAGE_INPUT_TOKENS));
        assertEquals(5L, span.getAttributes().get(GenAISpanHelper.GEN_AI_USAGE_OUTPUT_TOKENS));
        assertEquals(15L, span.getAttributes().get(GenAISpanHelper.GEN_AI_USAGE_TOTAL_TOKENS));

        // Check events
        assertTrue(span.getEvents().stream().anyMatch(e -> e.getName().equals("gen_ai.content.prompt")));
        assertTrue(span.getEvents().stream().anyMatch(e -> e.getName().equals("gen_ai.content.completion")));
    }

    @Test
    void traceCompletion_onError_setsErrorStatus() {
        assertThrows(RuntimeException.class, () ->
                GenAISpanHelper.traceCompletion(
                        tracer, "openai", "gpt-4", "sys", "usr",
                        () -> { throw new RuntimeException("boom"); },
                        new GenAISpanHelper.TokenExtractor<>() {
                            @Override public String model(Object r) { return ""; }
                            @Override public String text(Object r) { return ""; }
                            @Override public long inputTokens(Object r) { return 0; }
                            @Override public long outputTokens(Object r) { return 0; }
                        }));

        var spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        assertEquals(io.opentelemetry.api.trace.StatusCode.ERROR, spans.get(0).getStatus().getStatusCode());
    }

    @Test
    void traceEmbedding_createsSpanWithAttributes() {
        var result = GenAISpanHelper.traceEmbedding(
                tracer, "ollama", "nomic-embed", 3,
                () -> "embeddings");

        assertEquals("embeddings", result);

        var spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());

        SpanData span = spans.get(0);
        assertEquals("gen_ai.embeddings", span.getName());
        assertEquals("ollama", span.getAttributes().get(GenAISpanHelper.GEN_AI_SYSTEM));
        assertEquals("nomic-embed", span.getAttributes().get(GenAISpanHelper.GEN_AI_REQUEST_MODEL));
        assertEquals(3L, span.getAttributes().get(AttributeKey.longKey("gen_ai.embedding.input_count")));
    }

    @Test
    void traceEmbedding_onError_setsErrorStatus() {
        assertThrows(RuntimeException.class, () ->
                GenAISpanHelper.traceEmbedding(
                        tracer, "ollama", "model", 1,
                        () -> { throw new RuntimeException("fail"); }));

        var spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        assertEquals(io.opentelemetry.api.trace.StatusCode.ERROR, spans.get(0).getStatus().getStatusCode());
    }
}
