package com.topicscanner.telemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Central class defining all pipeline metrics for TopicScanner.
 * Covers scanner metrics (4.1), pipeline stage metrics (4.2), and content metrics (4.3).
 */
@Component
public class PipelineMetrics {

    private static final AttributeKey<String> SOURCE = AttributeKey.stringKey("source");
    private static final AttributeKey<String> STAGE = AttributeKey.stringKey("stage");
    private static final AttributeKey<String> REASON = AttributeKey.stringKey("reason");
    private static final AttributeKey<String> CATEGORY = AttributeKey.stringKey("category");
    private static final AttributeKey<String> FORMAT = AttributeKey.stringKey("format");

    // Story 4.1 — Scanner Metrics
    private final LongCounter topicsScanned;
    private final LongCounter sourceRateLimit;
    private final DoubleHistogram sourceRateLimitDuration;

    // Story 4.2 — Pipeline Stage Metrics
    private final LongCounter topicsProcessed;
    private final LongCounter topicsRejected;
    private final DoubleHistogram pipelineStageDuration;

    // Story 4.3 — Content Metrics
    private final LongCounter contentExtracted;
    private final LongCounter contentSummarized;
    private final LongCounter contentClustered;
    private final AtomicLong activeClustersValue = new AtomicLong(0);

    public PipelineMetrics(TelemetryService telemetryService) {
        // 4.1
        this.topicsScanned = telemetryService.createCounter(
                "topicscanner.topics.scanned", "Number of topics scanned");
        this.sourceRateLimit = telemetryService.createCounter(
                "topicscanner.source.ratelimit", "Rate limit events by source");
        this.sourceRateLimitDuration = telemetryService.createHistogram(
                "topicscanner.source.ratelimit.duration", "Rate limit backoff wait time", "ms");

        // 4.2
        this.topicsProcessed = telemetryService.createCounter(
                "topicscanner.topics.processed", "Topics processed by pipeline stage");
        this.topicsRejected = telemetryService.createCounter(
                "topicscanner.topics.rejected", "Topics rejected by pipeline stage");
        this.pipelineStageDuration = telemetryService.createHistogram(
                "topicscanner.pipeline.stage.duration", "Pipeline stage processing duration", "ms");

        // 4.3
        this.contentExtracted = telemetryService.createCounter(
                "topicscanner.content.extracted", "Content items extracted");
        this.contentSummarized = telemetryService.createCounter(
                "topicscanner.content.summarized", "Content items summarized");
        this.contentClustered = telemetryService.createCounter(
                "topicscanner.content.clustered", "Content items clustered");

        // Gauge for active clusters
        telemetryService.createGauge(
                "topicscanner.clusters.active",
                "Number of active clusters",
                "{clusters}",
                measurement -> measurement.record((double) activeClustersValue.get()));
    }

    // 4.1 methods
    public void recordTopicsScanned(String source, long count) {
        topicsScanned.add(count, Attributes.of(SOURCE, source));
    }

    public void recordRateLimit(String source) {
        sourceRateLimit.add(1, Attributes.of(SOURCE, source));
    }

    public void recordRateLimitDuration(String source, double ms) {
        sourceRateLimitDuration.record(ms, Attributes.of(SOURCE, source));
    }

    // 4.2 methods
    public void recordProcessed(String stage) {
        topicsProcessed.add(1, Attributes.of(STAGE, stage));
    }

    public void recordRejected(String stage, String reason) {
        topicsRejected.add(1, Attributes.of(STAGE, stage, REASON, reason));
    }

    public void recordStageDuration(String stage, double ms) {
        pipelineStageDuration.record(ms, Attributes.of(STAGE, stage));
    }

    // 4.3 methods
    public void recordExtracted(String source, String category) {
        contentExtracted.add(1, Attributes.of(SOURCE, source, CATEGORY, category));
    }

    public void recordSummarized(String format) {
        contentSummarized.add(1, Attributes.of(FORMAT, format));
    }

    public void recordClustered() {
        contentClustered.add(1);
    }

    public void setActiveClusters(long count) {
        activeClustersValue.set(count);
    }
}
