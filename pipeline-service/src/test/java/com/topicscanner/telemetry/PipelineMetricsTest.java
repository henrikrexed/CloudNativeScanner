package com.topicscanner.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class PipelineMetricsTest {

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
    }

    @Test
    void testRecordTopicsScanned() {
        pipelineMetrics.recordTopicsScanned("reddit", 5);
        pipelineMetrics.recordTopicsScanned("youtube", 3);

        Collection<MetricData> metrics = metricReader.collectAllMetrics();
        assertTrue(metrics.stream().anyMatch(m -> m.getName().equals("topicscanner.topics.scanned")));
    }

    @Test
    void testRecordRateLimit() {
        pipelineMetrics.recordRateLimit("reddit");

        Collection<MetricData> metrics = metricReader.collectAllMetrics();
        assertTrue(metrics.stream().anyMatch(m -> m.getName().equals("topicscanner.source.ratelimit")));
    }

    @Test
    void testRecordRateLimitDuration() {
        pipelineMetrics.recordRateLimitDuration("reddit", 5000.0);

        Collection<MetricData> metrics = metricReader.collectAllMetrics();
        assertTrue(metrics.stream().anyMatch(m -> m.getName().equals("topicscanner.source.ratelimit.duration")));
    }

    @Test
    void testRecordProcessed() {
        pipelineMetrics.recordProcessed("relevance-filter");

        Collection<MetricData> metrics = metricReader.collectAllMetrics();
        assertTrue(metrics.stream().anyMatch(m -> m.getName().equals("topicscanner.topics.processed")));
    }

    @Test
    void testRecordRejected() {
        pipelineMetrics.recordRejected("quality-filter", "low-score");

        Collection<MetricData> metrics = metricReader.collectAllMetrics();
        assertTrue(metrics.stream().anyMatch(m -> m.getName().equals("topicscanner.topics.rejected")));
    }

    @Test
    void testRecordStageDuration() {
        pipelineMetrics.recordStageDuration("relevance-filter", 150.0);

        Collection<MetricData> metrics = metricReader.collectAllMetrics();
        assertTrue(metrics.stream().anyMatch(m -> m.getName().equals("topicscanner.pipeline.stage.duration")));
    }

    @Test
    void testRecordExtracted() {
        pipelineMetrics.recordExtracted("reddit", "technology");

        Collection<MetricData> metrics = metricReader.collectAllMetrics();
        assertTrue(metrics.stream().anyMatch(m -> m.getName().equals("topicscanner.content.extracted")));
    }

    @Test
    void testRecordSummarized() {
        pipelineMetrics.recordSummarized("markdown");

        Collection<MetricData> metrics = metricReader.collectAllMetrics();
        assertTrue(metrics.stream().anyMatch(m -> m.getName().equals("topicscanner.content.summarized")));
    }

    @Test
    void testRecordClustered() {
        pipelineMetrics.recordClustered();

        Collection<MetricData> metrics = metricReader.collectAllMetrics();
        assertTrue(metrics.stream().anyMatch(m -> m.getName().equals("topicscanner.content.clustered")));
    }

    @Test
    void testSetActiveClusters() {
        pipelineMetrics.setActiveClusters(42);

        Collection<MetricData> metrics = metricReader.collectAllMetrics();
        assertTrue(metrics.stream().anyMatch(m -> m.getName().equals("topicscanner.clusters.active")));
    }
}
