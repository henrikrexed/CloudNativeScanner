/**
 * OpenTelemetry initialization for Next.js webui
 * Provides proper browser-side tracing with automatic instrumentation
 */

import { WebTracerProvider } from '@opentelemetry/sdk-trace-web';
import { FetchInstrumentation } from '@opentelemetry/instrumentation-fetch';
import { XMLHttpRequestInstrumentation } from '@opentelemetry/instrumentation-xml-http-request';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { Resource } from '@opentelemetry/resources';
import { SEMRESATTRS_SERVICE_NAME, SEMRESATTRS_SERVICE_VERSION } from '@opentelemetry/semantic-conventions';
import { BatchSpanProcessor } from '@opentelemetry/sdk-trace-base';
import { diag, DiagConsoleLogger, DiagLogLevel } from '@opentelemetry/api';

let tracerProvider: WebTracerProvider | null = null;

/**
 * Initialize OpenTelemetry SDK for browser-side tracing
 * This should be called once when the app starts
 */
export function initOpenTelemetry(): void {
  // Only run in browser environment
  if (typeof window === 'undefined') {
    return;
  }

  // Don't initialize twice
  if (tracerProvider !== null) {
    return;
  }

  try {
    // Enable debug logging for troubleshooting
    if (process.env.NODE_ENV === 'development' || process.env.NEXT_PUBLIC_OTEL_DEBUG === 'true') {
      diag.setLogger(new DiagConsoleLogger(), DiagLogLevel.DEBUG);
      console.log('[OTEL] Debug logging enabled');
    }

    // Get OTLP endpoint from environment or use default
    const otlpEndpoint = process.env.NEXT_PUBLIC_OTEL_EXPORTER_OTLP_ENDPOINT || 
                        process.env.NEXT_PUBLIC_OTEL_TRACES_ENDPOINT ||
                        'http://localhost:4318/v1/traces';

    // Create OTLP trace exporter
    const traceExporter = new OTLPTraceExporter({
      url: otlpEndpoint,
      headers: {},
    });

    // Create resource with service information
    const resource = new Resource({
      [SEMRESATTRS_SERVICE_NAME]: 'cloud-native-scanner-webui',
      [SEMRESATTRS_SERVICE_VERSION]: process.env.NEXT_PUBLIC_APP_VERSION || '1.0.1',
    });

    // Initialize tracer provider with automatic instrumentation
    tracerProvider = new WebTracerProvider({
      resource,
    });

    // Register the trace exporter
    tracerProvider.addSpanProcessor(new BatchSpanProcessor(traceExporter));

    // Create instrumentations BEFORE registering tracer provider
    // This ensures they can hook into the global API
    const fetchInstrumentation = new FetchInstrumentation({
      propagateTraceHeaderCorsUrls: [
        /^https?:\/\/.*/, // Allow all URLs for trace propagation
      ],
      clearTimingResources: true,
    });
    
    const xhrInstrumentation = new XMLHttpRequestInstrumentation({
      propagateTraceHeaderCorsUrls: [
        /^https?:\/\/.*/, // Allow all URLs for trace propagation
      ],
    });

    // Register the tracer provider (this makes it the global provider)
    tracerProvider.register();

    // Enable instrumentations AFTER registering tracer provider
    // They will automatically use the global tracer provider
    fetchInstrumentation.enable();
    xhrInstrumentation.enable();

    console.log('[OTEL] OpenTelemetry SDK initialized for browser-side tracing');
    console.log('[OTEL] OTLP endpoint:', otlpEndpoint);
    console.log('[OTEL] Tracer provider registered:', tracerProvider !== null);
    console.log('[OTEL] Fetch instrumentation enabled:', fetchInstrumentation !== null);
    console.log('[OTEL] XHR instrumentation enabled:', xhrInstrumentation !== null);
  } catch (error) {
    console.error('[OTEL] Failed to initialize OpenTelemetry SDK:', error);
    console.error('[OTEL] Error details:', error instanceof Error ? error.stack : String(error));
  }
}

/**
 * Shutdown OpenTelemetry SDK (called on app unmount)
 */
export function shutdownOpenTelemetry(): void {
  if (tracerProvider !== null) {
    try {
      tracerProvider.shutdown();
      tracerProvider = null;
      console.log('[OTEL] OpenTelemetry SDK shut down');
    } catch (error) {
      console.error('[OTEL] Error shutting down OpenTelemetry SDK:', error);
    }
  }
}

