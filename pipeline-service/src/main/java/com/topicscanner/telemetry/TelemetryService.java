package com.topicscanner.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@ConditionalOnProperty(name = "topicscanner.telemetry.enabled", havingValue = "true", matchIfMissing = true)
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    private final Tracer tracer;
    private final Meter meter;

    public TelemetryService(
            OpenTelemetry openTelemetry,
            @Value("${topicscanner.telemetry.service-name:topicscanner-pipeline}") String serviceName) {
        this.tracer = openTelemetry.getTracer(serviceName);
        this.meter = openTelemetry.getMeter(serviceName);
        log.info("TelemetryService initialized for service: {}", serviceName);
    }

    public Tracer getTracer() {
        return tracer;
    }

    public Meter getMeter() {
        return meter;
    }

    public Span createSpan(String name) {
        return tracer.spanBuilder(name).startSpan();
    }

    public LongCounter createCounter(String name, String description) {
        return meter.counterBuilder(name)
                .setDescription(description)
                .build();
    }

    public DoubleHistogram createHistogram(String name, String description, String unit) {
        return meter.histogramBuilder(name)
                .setDescription(description)
                .setUnit(unit)
                .build();
    }

    public ObservableDoubleGauge createGauge(String name, String description, String unit,
                                              Consumer<io.opentelemetry.api.metrics.ObservableDoubleMeasurement> callback) {
        return meter.gaugeBuilder(name)
                .setDescription(description)
                .setUnit(unit)
                .buildWithCallback(callback::accept);
    }
}
