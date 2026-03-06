package com.topicscanner.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.trace.Span;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryServiceTest {

    @Test
    void shouldCreateTracerAndMeter() {
        TelemetryService service = new TelemetryService(OpenTelemetry.noop(), "test-service");

        assertNotNull(service.getTracer());
        assertNotNull(service.getMeter());
    }

    @Test
    void shouldCreateSpan() {
        TelemetryService service = new TelemetryService(OpenTelemetry.noop(), "test-service");

        Span span = service.createSpan("test-span");
        assertNotNull(span);
        span.end();
    }

    @Test
    void shouldCreateCounter() {
        TelemetryService service = new TelemetryService(OpenTelemetry.noop(), "test-service");

        LongCounter counter = service.createCounter("test.counter", "A test counter");
        assertNotNull(counter);
    }

    @Test
    void shouldCreateHistogram() {
        TelemetryService service = new TelemetryService(OpenTelemetry.noop(), "test-service");

        DoubleHistogram histogram = service.createHistogram("test.histogram", "A test histogram", "ms");
        assertNotNull(histogram);
    }

    @Test
    void noOpServiceShouldWork() {
        NoOpTelemetryService noOp = new NoOpTelemetryService();

        assertNotNull(noOp.getTracer());
        assertNotNull(noOp.getMeter());
        Span span = noOp.createSpan("noop-span");
        assertNotNull(span);
        span.end();
    }
}
