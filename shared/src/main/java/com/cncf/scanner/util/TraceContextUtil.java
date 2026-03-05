package com.cncf.scanner.util;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;

/**
 * Utility class for OpenTelemetry trace context propagation
 */
public class TraceContextUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(TraceContextUtil.class);
    
    private static final String TRACE_ID_KEY = "traceId";
    private static final String SPAN_ID_KEY = "spanId";
    
    /**
     * Extract trace context from headers and set in MDC for logging
     */
    public static Context extractTraceContext(Map<String, String> headers) {
        try {
            TextMapGetter<Map<String, String>> getter = new TextMapGetter<Map<String, String>>() {
                @Override
                public Iterable<String> keys(Map<String, String> carrier) {
                    return carrier.keySet();
                }
                
                @Override
                public String get(Map<String, String> carrier, String key) {
                    return carrier.get(key);
                }
            };
            
            OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
            TextMapPropagator propagator = openTelemetry.getPropagators().getTextMapPropagator();
            Context context = propagator.extract(Context.current(), headers, getter);
            
            // Set trace context in MDC for logging
            Span currentSpan = Span.fromContext(context);
            if (currentSpan.getSpanContext().isValid()) {
                String traceId = currentSpan.getSpanContext().getTraceId();
                String spanId = currentSpan.getSpanContext().getSpanId();
                
                MDC.put(TRACE_ID_KEY, traceId);
                MDC.put(SPAN_ID_KEY, spanId);
                
                logger.debug("Extracted trace context - TraceId: {}, SpanId: {}", traceId, spanId);
            }
            
            return context;
        } catch (Exception e) {
            logger.warn("Failed to extract trace context: {}", e.getMessage());
            return Context.current();
        }
    }
    
    /**
     * Inject trace context into headers for propagation
     * This method safely handles cases where context might not be available (e.g., in parallel streams)
     */
    public static void injectTraceContext(Map<String, String> headers) {
        try {
            // Check if we have a valid current context before attempting injection
            Context currentContext = Context.current();
            if (currentContext == null) {
                logger.debug("No current OpenTelemetry context available, skipping trace context injection");
                return;
            }
            
            // Verify that we have a valid span in the context
            Span currentSpan = Span.fromContext(currentContext);
            if (currentSpan == null || currentSpan == Span.getInvalid() || !currentSpan.getSpanContext().isValid()) {
                logger.debug("No valid span in current context, skipping trace context injection");
                return;
            }
            
            TextMapSetter<Map<String, String>> setter = new TextMapSetter<Map<String, String>>() {
                @Override
                public void set(Map<String, String> carrier, String key, String value) {
                    carrier.put(key, value);
                }
            };
            
            OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
            if (openTelemetry == null) {
                logger.debug("OpenTelemetry instance not available, skipping trace context injection");
                return;
            }
            
            TextMapPropagator propagator = openTelemetry.getPropagators().getTextMapPropagator();
            if (propagator == null) {
                logger.debug("TextMapPropagator not available, skipping trace context injection");
                return;
            }
            
            propagator.inject(currentContext, headers, setter);
            logger.debug("Successfully injected trace context into headers");
        } catch (Exception e) {
            // Log warning but don't throw - allow Kafka send to proceed without trace context
            logger.warn("Failed to inject trace context (non-fatal): {}", e.getMessage());
            if (logger.isDebugEnabled()) {
                logger.debug("Trace context injection failure details", e);
            }
        }
    }
    
    /**
     * Create a new span with trace context
     * The span will be created as a child of the current context
     */
    public static Span createSpan(String spanName, String operation) {
        try {
            OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
            Tracer tracer = openTelemetry.getTracer("cloud-native-scanner");
            
            // Get current context - if there's a parent span, this will be linked as a child
            Context currentContext = Context.current();
            
            Span span = tracer.spanBuilder(spanName)
                    .setParent(currentContext)  // Explicitly set parent to ensure proper linking
                    .setAttribute("operation", operation)
                    .setAttribute("service.name", getServiceName())
                    .startSpan();
            
            // Set trace context in MDC
            String traceId = span.getSpanContext().getTraceId();
            String spanId = span.getSpanContext().getSpanId();
            
            MDC.put(TRACE_ID_KEY, traceId);
            MDC.put(SPAN_ID_KEY, spanId);
            
            Span parentSpan = Span.fromContext(currentContext);
            if (parentSpan != null && parentSpan != Span.getInvalid() && parentSpan.getSpanContext().isValid()) {
                logger.debug("Created child span - Name: {}, TraceId: {}, SpanId: {}, Parent: {}", 
                        spanName, traceId, spanId, parentSpan.getSpanContext().getSpanId());
            } else {
                logger.debug("Created root span - Name: {}, TraceId: {}, SpanId: {}", spanName, traceId, spanId);
            }
            
            return span;
        } catch (Exception e) {
            logger.warn("Failed to create span: {}", e.getMessage());
            return Span.getInvalid();
        }
    }
    
    /**
     * End a span and clean up MDC
     */
    public static void endSpan(Span span) {
        try {
            if (span != null && span != Span.getInvalid()) {
                span.end();
                logger.debug("Ended span - TraceId: {}, SpanId: {}", 
                        span.getSpanContext().getTraceId(), 
                        span.getSpanContext().getSpanId());
            }
        } catch (Exception e) {
            logger.warn("Failed to end span: {}", e.getMessage());
        } finally {
            // Clean up MDC
            MDC.remove(TRACE_ID_KEY);
            MDC.remove(SPAN_ID_KEY);
        }
    }
    
    /**
     * Add attributes to current span
     */
    public static void addSpanAttributes(Map<String, Object> attributes) {
        try {
            Span currentSpan = Span.current();
            if (currentSpan != null && currentSpan != Span.getInvalid()) {
                attributes.forEach((key, value) -> {
                    if (value != null) {
                        currentSpan.setAttribute(key, value.toString());
                    }
                });
                logger.debug("Added {} attributes to current span", attributes.size());
            }
        } catch (Exception e) {
            logger.warn("Failed to add span attributes: {}", e.getMessage());
        }
    }
    
    /**
     * Record an event in the current span
     */
    public static void recordEvent(String eventName, Map<String, Object> attributes) {
        try {
            Span currentSpan = Span.current();
            if (currentSpan != null && currentSpan != Span.getInvalid()) {
                // Convert Map to Attributes
                AttributesBuilder attributesBuilder = Attributes.builder();
                attributes.forEach((key, value) -> {
                    if (value != null) {
                        attributesBuilder.put(key, value.toString());
                    }
                });
                currentSpan.addEvent(eventName, attributesBuilder.build());
                logger.debug("Recorded event: {} with {} attributes", eventName, attributes.size());
            }
        } catch (Exception e) {
            logger.warn("Failed to record event: {}", e.getMessage());
        }
    }
    
    /**
     * Get current trace ID from MDC
     */
    public static String getCurrentTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }
    
    /**
     * Get current span ID from MDC
     */
    public static String getCurrentSpanId() {
        return MDC.get(SPAN_ID_KEY);
    }
    
    /**
     * Set trace context in MDC for logging
     */
    public static void setTraceContextInMDC() {
        try {
            Span currentSpan = Span.current();
            if (currentSpan != null && currentSpan != Span.getInvalid()) {
                String traceId = currentSpan.getSpanContext().getTraceId();
                String spanId = currentSpan.getSpanContext().getSpanId();
                String traceFlags = currentSpan.getSpanContext().getTraceFlags().asHex();
                
                MDC.put(TRACE_ID_KEY, traceId);
                MDC.put(SPAN_ID_KEY, spanId);
                MDC.put("traceFlags", traceFlags);
                
                logger.debug("Set trace context in MDC - TraceId: {}, SpanId: {}, TraceFlags: {}", 
                        traceId, spanId, traceFlags);
            }
        } catch (Exception e) {
            logger.warn("Failed to set trace context in MDC: {}", e.getMessage());
        }
    }
    
    /**
     * Clear trace context from MDC
     */
    public static void clearTraceContextFromMDC() {
        MDC.remove(TRACE_ID_KEY);
        MDC.remove(SPAN_ID_KEY);
        MDC.remove("traceFlags");
    }
    
    /**
     * Log with trace context
     */
    public static void logWithTraceContext(Logger logger, String level, String message, Object... args) {
        setTraceContextInMDC();
        try {
            switch (level.toUpperCase()) {
                case "DEBUG":
                    logger.debug(message, args);
                    break;
                case "INFO":
                    logger.info(message, args);
                    break;
                case "WARN":
                    logger.warn(message, args);
                    break;
                case "ERROR":
                    logger.error(message, args);
                    break;
                default:
                    logger.info(message, args);
            }
        } finally {
            clearTraceContextFromMDC();
        }
    }
    
    /**
     * Get service name from system properties or environment
     */
    private static String getServiceName() {
        return System.getProperty("otel.service.name", 
                System.getenv().getOrDefault("OTEL_SERVICE_NAME", "cloud-native-scanner"));
    }
    
    /**
     * Result of ensureRootContext - contains both the scope and whether a new span was created
     */
    public static class RootContextResult {
        private final Scope scope;
        private final boolean spanCreated;
        
        public RootContextResult(Scope scope, boolean spanCreated) {
            this.scope = scope;
            this.spanCreated = spanCreated;
        }
        
        public Scope getScope() {
            return scope;
        }
        
        public boolean isSpanCreated() {
            return spanCreated;
        }
    }
    
    /**
     * Ensure a root span exists for the current context.
     * If no valid context exists, creates a new root span for CronJob execution.
     * This should be called at the very beginning of CronJob execution.
     * 
     * @param spanName Name for the root span (e.g., "cronjob-scan")
     * @param operation Operation identifier (e.g., "CRONJOB_SCAN")
     * @return A RootContextResult containing the scope and whether a new span was created. Returns null if context creation fails.
     */
    public static RootContextResult ensureRootContext(String spanName, String operation) {
        try {
            Context currentContext = Context.current();
            Span currentSpan = Span.fromContext(currentContext);
            
            // Check if we have a valid span in the current context
            if (currentSpan != null && currentSpan != Span.getInvalid() && currentSpan.getSpanContext().isValid()) {
                logger.debug("Valid OpenTelemetry context already exists - TraceId: {}, SpanId: {}", 
                        currentSpan.getSpanContext().getTraceId(),
                        currentSpan.getSpanContext().getSpanId());
                // Context exists, create a scope from it (but don't end the span - it's not ours)
                return new RootContextResult(currentContext.makeCurrent(), false);
            }
            
            // No valid context exists - create a root span for CronJob execution
            logger.debug("No valid OpenTelemetry context found, creating root span for CronJob: {}", spanName);
            
            OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
            if (openTelemetry == null) {
                logger.warn("OpenTelemetry instance not available, cannot create root context");
                return null;
            }
            
            Tracer tracer = openTelemetry.getTracer("topic-scanner", "1.28");
            Span rootSpan = tracer.spanBuilder(spanName)
                    .setAttribute("operation", operation)
                    .setAttribute("service.name", getServiceName())
                    .setAttribute("trigger.type", "cronjob")
                    .startSpan();
            
            Context rootContext = Context.current().with(rootSpan);
            Scope scope = rootContext.makeCurrent();
            
            // Set trace context in MDC for logging
            String traceId = rootSpan.getSpanContext().getTraceId();
            String spanId = rootSpan.getSpanContext().getSpanId();
            MDC.put(TRACE_ID_KEY, traceId);
            MDC.put(SPAN_ID_KEY, spanId);
            
            logger.info("Created root span for CronJob - Name: {}, TraceId: {}, SpanId: {}", 
                    spanName, traceId, spanId);
            
            return new RootContextResult(scope, true);
        } catch (Exception e) {
            logger.warn("Failed to create root OpenTelemetry context: {}", e.getMessage());
            if (logger.isDebugEnabled()) {
                logger.debug("Root context creation failure details", e);
            }
            return null;
        }
    }
    
    /**
     * End the root span and close its scope.
     * This should be called at the end of CronJob execution.
     * 
     * @param result The RootContextResult returned by ensureRootContext(). Can be null.
     * @param spanName Name of the span for logging purposes.
     */
    public static void endRootContext(RootContextResult result, String spanName) {
        try {
            if (result != null && result.getScope() != null) {
                Scope scope = result.getScope();
                
                // Only end the span if we created it (not if we used an existing context)
                if (result.isSpanCreated()) {
                    // Get the current span before closing the scope
                    Span currentSpan = Span.current();
                    if (currentSpan != null && currentSpan != Span.getInvalid()) {
                        String traceId = currentSpan.getSpanContext().getTraceId();
                        String spanId = currentSpan.getSpanContext().getSpanId();
                        
                        // End the span we created
                        currentSpan.end();
                        logger.info("Ended root span for CronJob - Name: {}, TraceId: {}, SpanId: {}", 
                                spanName, traceId, spanId);
                    }
                } else {
                    logger.debug("Using existing context, not ending span - span is managed by caller");
                }
                
                // Close the scope
                scope.close();
            }
        } catch (Exception e) {
            logger.warn("Failed to end root OpenTelemetry context: {}", e.getMessage());
        } finally {
            // Clean up MDC
            clearTraceContextFromMDC();
        }
    }
}
