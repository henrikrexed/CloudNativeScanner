package com.topicscanner.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitInterceptorTest {

    private RateLimitInterceptor interceptor;
    private PipelineMetrics pipelineMetrics;
    private InMemoryMetricReader metricReader;

    @BeforeEach
    void setUp() {
        metricReader = InMemoryMetricReader.create();
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(metricReader)
                .build();
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setMeterProvider(meterProvider)
                .build();
        TelemetryService telemetryService = new TelemetryService(openTelemetry, "test-service");
        pipelineMetrics = new PipelineMetrics(telemetryService);
        interceptor = new RateLimitInterceptor(pipelineMetrics);
    }

    @Test
    void testRateLimitDetected() throws IOException {
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("https://api.reddit.com/topics"));
        MockClientHttpResponse mockResponse = new MockClientHttpResponse(new byte[0], HttpStatus.TOO_MANY_REQUESTS);
        mockResponse.getHeaders().set("Retry-After", "60");

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        ClientHttpResponse response = interceptor.intercept(request, new byte[0], execution);

        assertEquals(429, response.getStatusCode().value());
        var metrics = metricReader.collectAllMetrics();
        assertTrue(metrics.stream().anyMatch(m -> m.getName().equals("topicscanner.source.ratelimit")));
        assertTrue(metrics.stream().anyMatch(m -> m.getName().equals("topicscanner.source.ratelimit.duration")));
    }

    @Test
    void testNonRateLimitResponse() throws IOException {
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("https://api.reddit.com/topics"));
        MockClientHttpResponse mockResponse = new MockClientHttpResponse(new byte[0], HttpStatus.OK);

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        ClientHttpResponse response = interceptor.intercept(request, new byte[0], execution);

        assertEquals(200, response.getStatusCode().value());
        var metrics = metricReader.collectAllMetrics();
        assertFalse(metrics.stream().anyMatch(m -> m.getName().equals("topicscanner.source.ratelimit")));
    }

    @Test
    void testExtractSource() {
        assertEquals("reddit", interceptor.extractSource(URI.create("https://api.reddit.com/topics")));
        assertEquals("youtube", interceptor.extractSource(URI.create("https://www.youtube.com/api")));
        assertEquals("unknown", interceptor.extractSource(URI.create("file:///local")));
    }
}
