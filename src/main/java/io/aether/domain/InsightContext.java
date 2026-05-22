package io.aether.domain;

import java.time.Instant;
import java.util.List;

public record InsightContext(
        List<SensorReading> recentWindow,
        List<ForecastPoint> forecast,
        List<AnomalyEvent> activeAnomalies,
        Location location,
        Instant asOf
) {}
