package io.aether.domain;

import java.time.Instant;

public record AnomalyEvent(
        SensorId sensorId,
        String location,
        Metric metric,
        Instant observedAt,
        Double value,
        String method,
        double score
) {}
