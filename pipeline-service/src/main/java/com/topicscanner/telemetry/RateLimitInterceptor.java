package com.topicscanner.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Spring RestTemplate interceptor that detects HTTP 429 (Too Many Requests)
 * responses and records rate-limit metrics via PipelineMetrics.
 */
@Component
public class RateLimitInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final PipelineMetrics pipelineMetrics;

    public RateLimitInterceptor(PipelineMetrics pipelineMetrics) {
        this.pipelineMetrics = pipelineMetrics;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {
        ClientHttpResponse response = execution.execute(request, body);

        if (response.getStatusCode().value() == 429) {
            String source = extractSource(request.getURI());
            String retryAfter = response.getHeaders().getFirst("Retry-After");

            pipelineMetrics.recordRateLimit(source);

            if (retryAfter != null) {
                try {
                    double retryMs = Double.parseDouble(retryAfter) * 1000;
                    pipelineMetrics.recordRateLimitDuration(source, retryMs);
                } catch (NumberFormatException ignored) {
                    // Retry-After might be a date, skip duration recording
                }
            }

            try {
                MDC.put("source", source);
                MDC.put("retryAfter", retryAfter != null ? retryAfter : "unknown");
                log.warn("Rate limited by source: {}, Retry-After: {}", source,
                        retryAfter != null ? retryAfter : "unknown");
            } finally {
                MDC.remove("source");
                MDC.remove("retryAfter");
            }
        }

        return response;
    }

    String extractSource(URI uri) {
        String host = uri.getHost();
        if (host == null) return "unknown";
        // Extract meaningful source name from host
        String[] parts = host.split("\\.");
        if (parts.length >= 2) {
            return parts[parts.length - 2];
        }
        return host;
    }
}
