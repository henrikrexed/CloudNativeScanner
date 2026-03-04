package com.topicscanner.llm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FallbackLLMServiceTest {

    /**
     * Stub LLM that always succeeds.
     */
    static class SuccessLLM implements LLMService {
        private final String provider;

        SuccessLLM(String provider) {
            this.provider = provider;
        }

        @Override
        public String getProvider() {
            return provider;
        }

        @Override
        public LLMResponse complete(LLMTaskType taskType, String systemPrompt, String userPrompt) {
            return new LLMResponse("Response from " + provider, provider + "-model");
        }

        @Override
        public List<float[]> embed(List<String> texts) {
            return texts.stream().map(t -> new float[]{1.0f, 2.0f, 3.0f}).toList();
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }

    /**
     * Stub LLM that always fails.
     */
    static class FailingLLM implements LLMService {
        private final String provider;

        FailingLLM(String provider) {
            this.provider = provider;
        }

        @Override
        public String getProvider() {
            return provider;
        }

        @Override
        public LLMResponse complete(LLMTaskType taskType, String systemPrompt, String userPrompt) {
            throw new LLMException(provider, "Connection refused");
        }

        @Override
        public List<float[]> embed(List<String> texts) {
            throw new LLMException(provider, "Connection refused");
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }

    /**
     * Stub LLM that is not available.
     */
    static class UnavailableLLM implements LLMService {
        @Override
        public String getProvider() {
            return "unavailable";
        }

        @Override
        public LLMResponse complete(LLMTaskType taskType, String systemPrompt, String userPrompt) {
            throw new LLMException("unavailable", "Not configured");
        }

        @Override
        public List<float[]> embed(List<String> texts) {
            throw new LLMException("unavailable", "Not configured");
        }

        @Override
        public boolean isAvailable() {
            return false;
        }
    }

    @Test
    void primarySucceeds_usesPrimary() {
        var service = new FallbackLLMService(new SuccessLLM("ollama"), new SuccessLLM("openai"));

        LLMResponse response = service.complete(LLMTaskType.RELEVANCE, "system", "user");

        assertEquals("Response from ollama", response.text());
    }

    @Test
    void primaryFails_fallbackSucceeds() {
        var service = new FallbackLLMService(new FailingLLM("ollama"), new SuccessLLM("openai"));

        LLMResponse response = service.complete(LLMTaskType.RELEVANCE, "system", "user");

        assertEquals("Response from openai", response.text());
    }

    @Test
    void bothFail_throws() {
        var service = new FallbackLLMService(new FailingLLM("ollama"), new FailingLLM("openai"));

        assertThrows(LLMException.class,
                () -> service.complete(LLMTaskType.RELEVANCE, "system", "user"));
    }

    @Test
    void primaryFails_noFallback_throws() {
        var service = new FallbackLLMService(new FailingLLM("ollama"), null);

        assertThrows(LLMException.class,
                () -> service.complete(LLMTaskType.RELEVANCE, "system", "user"));
    }

    @Test
    void primaryFails_fallbackUnavailable_throws() {
        var service = new FallbackLLMService(new FailingLLM("ollama"), new UnavailableLLM());

        assertThrows(LLMException.class,
                () -> service.complete(LLMTaskType.RELEVANCE, "system", "user"));
    }

    @Test
    void embed_primarySucceeds() {
        var service = new FallbackLLMService(new SuccessLLM("ollama"), new SuccessLLM("openai"));

        List<float[]> result = service.embed(List.of("hello"));

        assertEquals(1, result.size());
        assertEquals(3, result.get(0).length);
    }

    @Test
    void embed_primaryFails_fallsBackToCloud() {
        var service = new FallbackLLMService(new FailingLLM("ollama"), new SuccessLLM("openai"));

        List<float[]> result = service.embed(List.of("hello"));

        assertEquals(1, result.size());
    }

    @Test
    void isAvailable_primaryAvailable() {
        var service = new FallbackLLMService(new SuccessLLM("ollama"), new UnavailableLLM());
        assertTrue(service.isAvailable());
    }

    @Test
    void isAvailable_onlyFallbackAvailable() {
        var service = new FallbackLLMService(new UnavailableLLM(), new SuccessLLM("openai"));
        assertTrue(service.isAvailable());
    }

    @Test
    void isAvailable_noneAvailable() {
        var service = new FallbackLLMService(new UnavailableLLM(), new UnavailableLLM());
        assertFalse(service.isAvailable());
    }

    @Test
    void provider_showsBothNames() {
        var service = new FallbackLLMService(new SuccessLLM("ollama"), new SuccessLLM("openai"));
        assertEquals("ollama+openai", service.getProvider());
    }

    @Test
    void provider_nullFallback_showsNone() {
        var service = new FallbackLLMService(new SuccessLLM("ollama"), null);
        assertEquals("ollama+none", service.getProvider());
    }

    /**
     * Stub that throws UnsupportedOperationException on embed (like Claude).
     */
    static class NoEmbedLLM implements LLMService {
        @Override
        public String getProvider() {
            return "no-embed";
        }

        @Override
        public LLMResponse complete(LLMTaskType taskType, String systemPrompt, String userPrompt) {
            return new LLMResponse("ok", "model");
        }

        @Override
        public List<float[]> embed(List<String> texts) {
            throw new UnsupportedOperationException("No embedding support");
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }

    @Test
    void embed_primaryFails_fallbackUnsupported_throws() {
        var service = new FallbackLLMService(new FailingLLM("ollama"), new NoEmbedLLM());

        assertThrows(LLMException.class,
                () -> service.embed(List.of("hello")));
    }

    @Test
    void embed_bothFail_throws() {
        var service = new FallbackLLMService(new FailingLLM("ollama"), new FailingLLM("openai"));

        assertThrows(LLMException.class,
                () -> service.embed(List.of("hello")));
    }

    @Test
    void embed_noFallback_throws() {
        var service = new FallbackLLMService(new FailingLLM("ollama"), null);

        assertThrows(LLMException.class,
                () -> service.embed(List.of("hello")));
    }

    @Test
    void getPrimary_returnsPrimary() {
        var primary = new SuccessLLM("ollama");
        var service = new FallbackLLMService(primary, new SuccessLLM("openai"));

        assertSame(primary, service.getPrimary());
    }

    @Test
    void getFallback_returnsFallback() {
        var fallback = new SuccessLLM("openai");
        var service = new FallbackLLMService(new SuccessLLM("ollama"), fallback);

        assertSame(fallback, service.getFallback());
    }
}
