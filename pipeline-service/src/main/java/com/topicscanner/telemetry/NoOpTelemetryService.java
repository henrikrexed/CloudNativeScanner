package com.topicscanner.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "topicscanner.telemetry.enabled", havingValue = "false")
public class NoOpTelemetryService extends TelemetryService {

    public NoOpTelemetryService() {
        super(OpenTelemetry.noop(), "noop");
    }
}
