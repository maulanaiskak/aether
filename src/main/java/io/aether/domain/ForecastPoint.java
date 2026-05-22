package io.aether.domain;

import java.time.Instant;

public record ForecastPoint(
        Instant horizonAt,
        double predicted,
        double lowerBound,
        double upperBound
) {}
