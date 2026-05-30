package io.aether.api.dto;



public record ForecastMetricsDto(String location, String metric, String model, double mae, double rmse, String evaluatedAt) {}
