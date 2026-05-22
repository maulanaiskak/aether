package io.aether.api.dto;

import java.time.Instant;

public record ForecastPointDto(Instant horizonAt, double predicted, double lowerBound, double upperBound) {}
