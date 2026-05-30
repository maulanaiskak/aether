package io.aether.api.dto;

public record AnomalyEventDto(
        String sensorId,
        String location,
        String metric,
        String observedAt,
        Double value,
        String method,
        double score
) {}
