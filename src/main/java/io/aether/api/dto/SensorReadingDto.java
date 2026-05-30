package io.aether.api.dto;

public record SensorReadingDto(
        String sensorId,
        String location,
        String metric,
        String unit,
        Double value,
        Double smoothedValue,
        String observedAt,
        String qualityStatus
) {}
