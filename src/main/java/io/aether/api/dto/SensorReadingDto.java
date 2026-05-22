package io.aether.api.dto;

import java.time.Instant;

public record SensorReadingDto(
        String sensorId,
        String location,
        String metric,
        String unit,
        Double value,
        Double smoothedValue,
        Instant observedAt,
        String qualityStatus
) {}
