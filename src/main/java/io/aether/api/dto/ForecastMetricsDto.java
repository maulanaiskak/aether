package io.aether.api.dto;

import java.time.Instant;

public record ForecastMetricsDto(String location, String metric, String model, double mae, double rmse, Instant evaluatedAt) {}
