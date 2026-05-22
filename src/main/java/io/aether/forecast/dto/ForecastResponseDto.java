package io.aether.forecast.dto;

import java.util.List;

public record ForecastResponseDto(
        String location,
        String model,
        List<Prediction> predictions
) {
    public record Prediction(String horizonAt, double predicted, double lowerBound, double upperBound) {}
}
