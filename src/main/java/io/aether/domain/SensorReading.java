package io.aether.domain;

import java.time.Instant;

public record SensorReading(
        SensorId sensorId,
        int schemaVersion,
        String location,
        Metric metric,
        String unit,
        Double value,
        Instant observedAt,
        Instant ingestedAt,
        String source,
        Quality quality
) {}
