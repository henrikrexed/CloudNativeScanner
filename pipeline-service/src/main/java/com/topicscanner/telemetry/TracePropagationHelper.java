package com.topicscanner.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;

import java.util.Map;

/**
 * Propagates trace context through pipeline job metadata using W3C traceparent format.
 */
public class TracePropagationHelper {

    private static final String TRACEPARENT_KEY = "traceparent";

    private TracePropagationHelper() {}

    /**
     * Injects current span's trace context into the metadata map as a W3C traceparent string.
     */
    public static void injectTraceContext(Map<String, Object> metadata) {
        if (metadata == null) return;

        Span currentSpan = Span.current();
        SpanContext ctx = currentSpan.getSpanContext();
        if (!ctx.isValid()) return;

        String traceparent = String.format("00-%s-%s-%s",
                ctx.getTraceId(), ctx.getSpanId(), ctx.getTraceFlags().asHex());
        metadata.put(TRACEPARENT_KEY, traceparent);
    }

    /**
     * Extracts trace context from metadata map and returns an OTel Context with the parent span.
     * Returns Context.root() if no valid traceparent is found.
     */
    public static Context extractTraceContext(Map<String, Object> metadata) {
        if (metadata == null) return Context.root();

        Object traceparentObj = metadata.get(TRACEPARENT_KEY);
        if (traceparentObj == null) return Context.root();

        String traceparent = traceparentObj.toString();
        String[] parts = traceparent.split("-");
        if (parts.length != 4 || !"00".equals(parts[0])) return Context.root();

        String traceId = parts[1];
        String spanId = parts[2];
        String flags = parts[3];

        if (traceId.length() != 32 || spanId.length() != 16) return Context.root();

        SpanContext spanContext = SpanContext.createFromRemoteParent(
                traceId, spanId,
                TraceFlags.fromHex(flags, 0),
                TraceState.getDefault());

        if (!spanContext.isValid()) return Context.root();

        return Context.root().with(Span.wrap(spanContext));
    }
}
