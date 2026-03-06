package com.topicscanner.llm;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FallbackLLMServiceTest {

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

    static class SuccessLLM implements LLMService {
        private final String provider;
        SuccessLLM(String provider) { this.provider = provider; }
        @Override public String getProvider() { return provider; }
        @Override public LLMResponse complete(LLMTaskType taskType, String systemPrompt, String userPrompt) {
            return new LLMResponse("Response from " + provider, provider + "-model");
        }
        @Override public List<float[]> embed(List<String> texts) {
            return texts.stream().map(t -> new float[]{1.0f, 2.0f, 3.0f}).toList();
        }
        @Override public boolean isAvailable() { return true; }
    }

    static class FailingLLM implements LLMService {
        private final String provider;
        FailingLLM(String provider) { this.provider = provider; }
        @Override public String getProvider() { return provider; }
        @Override public LLMResponse complete(LLMTaskType taskType, String systemPrompt, String userPrompt) {
            throw new LLMException(provider, "Connection refused");
        }
        @Override public List<float[]> embed(List<String> texts) {
            throw new LLMException(provider, "Connection refused");
        }
        @Override public boolean isAvailable() { return true; }
    }

    static class UnavailableLLM implements LLMService {
        @Override public String getProvider() { return "unavailable"; }
        @Override public LLMResponse complete(LLMTaskType taskType, String systemPrompt, String userPrompt) {
            throw new LLMException("unavailable", "Not configured");
        }
        @Override public List<float[]> embed(List<String> texts) {
            throw new LLMException("unavailable", "Not configured");
        }
        @Override public boolean isAvailable() { return false; }
    }

    @Test
    void primarySucceeds_usesPrimary() {
        var service = new FallbackLLMService(new SuccessLLM("ollama"), new SuccessLLM("openai"), tracer);
        LLMResponse response = service.complete(LLMTaskType.RELEVANCE, "system", "user");
        assertEquals("Response from ollama", response.text());
    }

    @Test
    void primaryFails_fallbackSucceeds() {
        var service = new FallbackLLMService(new FailingLLM("ollama"), new SuccessLLM("openai"), tracer);
        LLMResponse response = service.complete(LLMTaskType.RELEVANCE, "system", "user");
        assertEquals("Response from openai", response.text());
    }

    @Test
    void bothFail_throws() {
        var service = new FallbackLLMService(new FailingLLM("ollama"), new FailingLLM("openai"), tracer);
        assertThrows(LLMException.class, () -> service.complete(LLMTaskType.RELEVANCE, "system", "user"));
    }

    @Test
    void primaryFails_noFallback_throws() {
        var service = new FallbackLLMService(new FailingLLM("ollama"), null, tracer);
        assertThrows(LLMException.class, () -> service.complete(LLMTaskType.RELEVANCE, "system", "user"));
    }

    @Test
    void primaryFails_fallbackUnavailable_throws() {
        var service = new FallbackLLMService(new FailingLLM("ollama"), new UnavailableLLM(), tracer);
        assertThrows(LLMException.class, () -> service.complete(LLMTaskType.RELEVANCE, "system", "user"));
    }

    @Test
    void embed_primarySucceeds() {
        var service = new FallbackLLMService(new SuccessLLM("ollama"), new SuccessLLM("openai"), tracer);
        List<float[]> result = service.embed(List.of("hello"));
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).length);
    }

    @Test
    void embed_primaryFails_fallsBackToCloud() {
        var service = new FallbackLLMService(new FailingLLM("ollama"), new SuccessLLM("openai"), tracer);
        List<float[]> result = service.embed(List.of("hello"));
        assertEquals(1, result.size());
    }

    @Test
    void isAvailable_primaryAvailable() {
        var service = new FallbackLLMService(new SuccessLLM("ollama"), new UnavailableLLM(), tracer);
        assertTrue(service.isAvailable());
    }

    @Test
    void isAvailable_onlyFallbackAvailable() {
        var service = new FallbackLLMService(new UnavailableLLM(), new SuccessLLM("openai"), tracer);
        assertTrue(service.isAvailable());
    }

    @Test
    void isAvailable_noneAvailable() {
        var service = new FallbackLLMService(new UnavailableLLM(), new UnavailableLLM(), tracer);
        assertFalse(service.isAvailable());
    }

    @Test
    void provider_showsBothNames() {
        var service = new FallbackLLMService(new SuccessLLM("ollama"), new SuccessLLM("openai"), tracer);
        assertEquals("ollama+openai", service.getProvider());
    }

    @Test
    void provider_nullFallback_showsNone() {
        var service = new FallbackLLMService(new SuccessLLM("ollama"), null, tracer);
        assertEquals("ollama+none", service.getProvider());
    }

    @Test
    void fallbackCreatesSpan() {
        var service = new FallbackLLMService(new SuccessLLM("ollama"), new SuccessLLM("openai"), tracer);
        service.complete(LLMTaskType.RELEVANCE, "system", "user");
        assertFalse(spanExporter.getFinishedSpanItems().isEmpty());
        assertEquals("llm.fallback.complete", spanExporter.getFinishedSpanItems().get(0).getName());
    }

    @Test
    void fallbackCreatesSpanOnFailover() {
        var service = new FallbackLLMService(new FailingLLM("ollama"), new SuccessLLM("openai"), tracer);
        service.complete(LLMTaskType.RELEVANCE, "system", "user");

        var spans = spanExporter.getFinishedSpanItems();
        assertFalse(spans.isEmpty());
        var span = spans.get(0);
        assertEquals("llm.fallback.complete", span.getName());
        // Should have a "primary_failed" event
        assertTrue(span.getEvents().stream().anyMatch(e -> e.getName().equals("primary_failed")));
        assertTrue(span.getEvents().stream().anyMatch(e -> e.getName().equals("fallback_succeeded")));
    }

    @Test
    void getPrimary_returnsPrimary() {
        var primary = new SuccessLLM("ollama");
        var service = new FallbackLLMService(primary, new SuccessLLM("openai"), tracer);
        assertSame(primary, service.getPrimary());
    }

    @Test
    void getFallback_returnsFallback() {
        var fallback = new SuccessLLM("openai");
        var service = new FallbackLLMService(new SuccessLLM("ollama"), fallback, tracer);
        assertSame(fallback, service.getFallback());
    }

    @Test
    void embed_noFallback_throws() {
        var service = new FallbackLLMService(new FailingLLM("ollama"), null, tracer);
        assertThrows(LLMException.class, () -> service.embed(List.of("hello")));
    }

    @Test
    void embed_bothFail_throws() {
        var service = new FallbackLLMService(new FailingLLM("ollama"), new FailingLLM("openai"), tracer);
        assertThrows(LLMException.class, () -> service.embed(List.of("hello")));
    }
}
