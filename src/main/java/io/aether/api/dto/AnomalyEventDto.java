package io.aether.api.dto;

import java.time.Instant;

public record AnomalyEventDto(
        String sensorId,
        String location,
        String metric,
        Instant observedAt,
        Double value,
        String method,
        double score
) {}
