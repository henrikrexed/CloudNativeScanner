package com.topicscanner.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TracePropagationHelperTest {

    @Test
    void roundTripInjectExtract() {
        // Create a fake span context and make it current
        SpanContext parentCtx = SpanContext.createFromRemoteParent(
                "0af7651916cd43dd8448eb211c80319c",
                "b7ad6b7169203331",
                TraceFlags.getSampled(),
                TraceState.getDefault());
        Span fakeSpan = Span.wrap(parentCtx);

        try (var scope = Context.root().with(fakeSpan).makeCurrent()) {
            Map<String, Object> metadata = new HashMap<>();
            TracePropagationHelper.injectTraceContext(metadata);

            assertTrue(metadata.containsKey("traceparent"));
            String traceparent = (String) metadata.get("traceparent");
            assertTrue(traceparent.startsWith("00-0af7651916cd43dd8448eb211c80319c-"));

            // Extract and verify trace ID matches
            Context extracted = TracePropagationHelper.extractTraceContext(metadata);
            Span extractedSpan = Span.fromContext(extracted);
            assertEquals("0af7651916cd43dd8448eb211c80319c",
                    extractedSpan.getSpanContext().getTraceId());
        }
    }

    @Test
    void extractFromNullMetadata() {
        Context ctx = TracePropagationHelper.extractTraceContext(null);
        assertNotNull(ctx);
        assertFalse(Span.fromContext(ctx).getSpanContext().isValid());
    }

    @Test
    void injectWithNullMetadata() {
        // Should not throw
        TracePropagationHelper.injectTraceContext(null);
    }

    @Test
    void extractFromEmptyMetadata() {
        Context ctx = TracePropagationHelper.extractTraceContext(new HashMap<>());
        assertNotNull(ctx);
        assertFalse(Span.fromContext(ctx).getSpanContext().isValid());
    }

    @Test
    void extractFromInvalidTraceparent() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("traceparent", "invalid-format");
        Context ctx = TracePropagationHelper.extractTraceContext(metadata);
        assertFalse(Span.fromContext(ctx).getSpanContext().isValid());
    }
}
