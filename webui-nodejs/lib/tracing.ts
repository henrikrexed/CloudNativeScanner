/**
 * OpenTelemetry tracing utilities for Next.js webui
 * Provides trace context propagation for API calls
 */

// Simple traceparent header generation and propagation
// This is a minimal implementation - for production, consider using @opentelemetry/api

interface TraceContext {
  traceId: string
  spanId: string
  traceFlags: string
}

/**
 * Generate a new trace context (traceId and spanId)
 */
function generateTraceContext(): TraceContext {
  // Generate 32-character hex traceId (16 bytes)
  const traceId = Array.from(crypto.getRandomValues(new Uint8Array(16)))
    .map(b => b.toString(16).padStart(2, '0'))
    .join('')
  
  // Generate 16-character hex spanId (8 bytes)
  const spanId = Array.from(crypto.getRandomValues(new Uint8Array(8)))
    .map(b => b.toString(16).padStart(2, '0'))
    .join('')
  
  // Trace flags: 01 = sampled
  const traceFlags = '01'
  
  return { traceId, spanId, traceFlags }
}

/**
 * Get or create trace context from sessionStorage
 * This ensures trace continuity across page navigations within the same session
 */
function getOrCreateTraceContext(): TraceContext {
  if (typeof window === 'undefined') {
    // Server-side: generate new context
    return generateTraceContext()
  }
  
  try {
    const stored = sessionStorage.getItem('traceContext')
    if (stored) {
      const context = JSON.parse(stored)
      // Validate format
      if (context.traceId && context.spanId && context.traceFlags) {
        return context
      }
    }
  } catch (e) {
    // Invalid stored context, generate new one
  }
  
  // Generate new context
  const context = generateTraceContext()
  
  // Store in sessionStorage for continuity
  try {
    sessionStorage.setItem('traceContext', JSON.stringify(context))
  } catch (e) {
    // sessionStorage might not be available (e.g., private browsing)
  }
  
  return context
}

/**
 * Create a new child span ID (for nested operations)
 */
function createChildSpanId(): string {
  return Array.from(crypto.getRandomValues(new Uint8Array(8)))
    .map(b => b.toString(16).padStart(2, '0'))
    .join('')
}

/**
 * Generate traceparent header value (W3C Trace Context format)
 * Format: version-traceId-parentId-traceFlags
 * Example: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
 */
export function getTraceparentHeader(spanId?: string): string {
  const context = getOrCreateTraceContext()
  const parentId = spanId || context.spanId
  
  // W3C Trace Context format: version-traceId-parentId-traceFlags
  return `00-${context.traceId}-${parentId}-${context.traceFlags}`
}

/**
 * Inject traceparent header into request headers
 */
export function injectTraceContext(headers: Record<string, string>): void {
  const traceparent = getTraceparentHeader()
  headers['traceparent'] = traceparent
  
  // Also set tracestate if needed (empty for now)
  // headers['tracestate'] = ''
}

/**
 * Get current trace ID (for logging/debugging)
 */
export function getCurrentTraceId(): string | null {
  if (typeof window === 'undefined') {
    return null
  }
  
  try {
    const stored = sessionStorage.getItem('traceContext')
    if (stored) {
      const context = JSON.parse(stored)
      return context.traceId || null
    }
  } catch (e) {
    // Ignore
  }
  
  return null
}

/**
 * Create a new span for an operation (returns new spanId)
 * This can be used to track nested operations
 */
export function createSpan(): string {
  return createChildSpanId()
}

/**
 * Initialize tracing (called on app startup)
 */
export function initTracing(): void {
  if (typeof window === 'undefined') {
    return // Server-side, skip
  }
  
  // Initialize trace context on page load
  getOrCreateTraceContext()
  
  // Log trace ID in development mode
  if (process.env.NODE_ENV === 'development') {
    const traceId = getCurrentTraceId()
    if (traceId) {
      console.log('[OTEL] Trace ID:', traceId)
    }
  }
}

