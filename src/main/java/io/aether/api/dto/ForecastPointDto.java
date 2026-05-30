package io.aether.api.dto;



public record ForecastPointDto(String horizonAt, double predicted, double lowerBound, double upperBound) {}
